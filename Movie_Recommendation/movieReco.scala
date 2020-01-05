// https://grouplens.org/datasets/movielens/ --> ml-latest-small
// Data collected from the MovieLens (web-based recommender system)

// --> from spark-2.3.4-bin-hadoop2.7/bin:

// *** PREPROCESSING ***
// Import data
val df_ratings = spark.read.option("header", false).csv("ml-latest-small/ratings.csv")
val df_movies = spark.read.option("header", false).csv("ml-latest-small/movies.csv")
// Note: spark is the SparkSession allowing us to use the DataFrame API
// SparkSession is the only entry point for all API

// Removing header of the csv file
val header=df_ratings.first
val df_ratings_2=df_ratings.filter(line => line != header)

val header_2 = df_movies.first
val df_movies_2 = df_movies.filter(line => line != header_2)

// Cast third column to float
import org.apache.spark.sql._
val df_ratings_3 =  df_ratings_2.withColumn("_c2", $"_c2".cast("Float")) // sql implicits converts $"col name" into a column

// Compute rating average for all movies
val df_top_movies = df_ratings_3.groupBy($"_c1")
.agg(avg($"_c2").as("rating_avg"))
.sort($"rating_avg".desc)
.limit(10)

// *** BEST MOVIES ***
// Display name of the best 10 movies
val df_top_movies_2 = df_top_movies.join(df_movies_2, df_top_movies("_c1")===df_movies_2("_c0")).show(false)
// Result not really interesting because it shows movies that have been rated only once with 5 stars...

// Rename columns (could be done earlier...)
val df_ratings_4=df_ratings_3
.withColumnRenamed("_c0","userId")
.withColumnRenamed("_c1","movieId")
.withColumnRenamed("_c2","rating")
.withColumnRenamed("_c3","timestamp")

val df_movies_3=df_movies_2
.withColumnRenamed("_c0","movieId")
.withColumnRenamed("_c1","title")
.withColumnRenamed("_c2","genre")

// Better ordered list
// grade_formula_1 --> the numerator is increasing higher that the denominator when the number of grades increases
// => it allows to penalise the movies that has only few grades
// grade_formula_2 --> similar to the previous formula, but we use the logarithm in order to penalise movies
// with few grades and harmonize grades of those with many grades
val df_temp_1 = df_ratings_4.groupBy("movieId").agg(sum("rating"),count("movieId")).toDF("movieId","sum_ratings","nb_ratings")
var df_grades = df_temp_1.withColumn("grade_1",$"sum_ratings"/($"nb_ratings"+1))
df_grades = df_grades.withColumn("grade_2",($"sum_ratings"/($"nb_ratings"))*log($"nb_ratings")) // (base exp logarithm)
// Display top 10 with names according to grade 1
val df_grades_1 = df_grades.join(df_movies_3,df_grades("movieId")===df_movies_3("movieId")).select("nb_ratings","grade_1","title").orderBy(desc("grade_1"))
df_grades_1.show(10, false)
// Display top 10 with names according to grade 2
val df_grades_2 = df_grades.join(df_movies_3,df_grades("movieId")===df_movies_3("movieId")).select("nb_ratings","grade_2","title").orderBy(desc("grade_2"))
df_grades_2.show(10, false)
// --> We note some similarities in the grade order with movies appearing in using both grading formulas including The Godfather, The Shawshank Redemption and Fight Club

// *** SIMILARITY ***
// Add column of tuples
val dfRatingWithList = df_ratings_4.groupBy("userId").agg(collect_list("movieId").alias("userMovies"),collect_list("rating").alias("userRatings"))

import scala.collection.mutable.WrappedArray
def movieToRating(warr1: WrappedArray[Any],warr2: WrappedArray[Any]): Map[String, Float] = {
    val arr1 = warr1.asInstanceOf[WrappedArray[String]].toArray  
    val arr2 = warr2.asInstanceOf[WrappedArray[Float]].toArray
    (arr1 zip arr2).toMap
}
val movieToRatingUdf = udf(movieToRating _)
val dfRatingWithMap = dfRatingWithList.withColumn("moviesToRatings",movieToRatingUdf($"userMovies",$"userRatings"))

// Get Map for user 1
val t = dfRatingWithMap.filter($"userId"==="1").select("moviesToRatings").collect.map(_.toSeq).flatten
val MAP_USER = t(0).asInstanceOf[Map[String, Float]] // will be used in UDF functions (TODO: make it as a parameter)

// Compute Pearson correlation
def simil(mapUserCol: Map[String, Float]): Option[Double] = {

        // Common movies
        val listOfCommonMovies = (MAP_USER.keySet & mapUserCol.keySet).toSeq
        val n = listOfCommonMovies.size
        if (n == 0) return Some(0.0)
        if (MAP_USER == mapUserCol) return Some(0.0) // when one user is compared with himself

        // filter the maps with those movies
        val mapUserCommonMoviesRatings = MAP_USER.filterKeys(movie => listOfCommonMovies.contains(movie))
        val mapUserColCommonMoviesRatings = mapUserCol.filterKeys(movie => listOfCommonMovies.contains(movie))

        // sum
        val sum1 = mapUserCommonMoviesRatings.values.sum
        val sum2 = mapUserColCommonMoviesRatings.values.sum

        // sum squared
        val sum1Sq = mapUserCommonMoviesRatings.values.foldLeft(0.0)(_ + Math.pow(_, 2))
        val sum2Sq = mapUserColCommonMoviesRatings.values.foldLeft(0.0)(_ + Math.pow(_, 2))

        // sum product
        val pSum = listOfCommonMovies.foldLeft(0.0)((accum, element) => accum + mapUserCommonMoviesRatings(element) * mapUserColCommonMoviesRatings(element))

        // Pearson score with log factor
        // Note: König-Huygens Theorem is used for calculus simplification
        val numerator = pSum - (sum1*sum2/n)
        val denominator = Math.sqrt( (sum1Sq-Math.pow(sum1,2)/n) * (sum2Sq-Math.pow(sum2,2)/n))
        if (denominator == 0 || numerator < 0) Some(0.0) else Some(numerator/denominator*Math.log(1+n)) // see BEST MOVIES section for log explanation
}
val similUdf = udf(simil _)
val dfRatingWithCorrel = dfRatingWithMap.withColumn("similWithUser1",similUdf($"moviesToRatings"))

// Similarity with user 3
dfRatingWithCorrel.select("userId","similWithUser1").filter($"userId"==="3").show(5)

// *** GRADE OF A MOVIE ***
val dfRatingCorrel = dfRatingWithCorrel.select("userId","moviesToRatings","similWithUser1")

// Alpha 
val ALPHA = dfRatingCorrel.agg(sum($"similWithUser1")).head.getDouble(0) / dfRatingCorrel.count

// Explode map and aggregate movies / ratings in arrays
val dfRatingCorrelExpl = dfRatingCorrel.select(explode($"moviesToRatings"),$"similWithUser1").withColumnRenamed("key","movieId").withColumnRenamed("value","rating")
val dfRatingCorrelGrouped = dfRatingCorrelExpl.groupBy("movieId").agg(collect_list("rating").alias("ratings"),collect_list("similWithUser1").alias("similarities"))

// Sum product ratings / similarities
def sumProdRatingSimil(warr1: WrappedArray[Any],warr2: WrappedArray[Any]): Double = {

    val arr1 = warr1.asInstanceOf[WrappedArray[Float]].toArray  
    val arr2 = warr2.asInstanceOf[WrappedArray[Double]].toArray

    val sumP = (arr1,arr2).zipped.map(_ * _).sum
    val sumSimil = arr2.sum

    sumP / (0.5 + sumSimil) // We assume ALPHA = 0.5

}
val sumProdRatingSimilUdf = udf(sumProdRatingSimil _)
val dfRatingCorrelScore = dfRatingCorrelGrouped.withColumn("score",sumProdRatingSimilUdf($"ratings",$"similarities"))

// 20 recommended movies for user 1
dfRatingCorrelScore.join(df_movies_3, dfRatingCorrelScore("movieId")===df_movies_3("movieId")).select("title","score").orderBy(desc("score")).show(20)

// 20 recommended movies for user 1 that he/she has not rated
val dfRatingCorrelGroupedNeverSeen = dfRatingCorrelGrouped.filter(!$"movieId".isin(MAP_USER.keySet.toArray: _*))
val dfRatingCorrelScoreNeverSeen = dfRatingCorrelGroupedNeverSeen.withColumn("score",sumProdRatingSimilUdf($"ratings",$"similarities"))

dfRatingCorrelScoreNeverSeen.join(df_movies_3, dfRatingCorrelScoreNeverSeen("movieId")===df_movies_3("movieId")).select("title","score").orderBy(desc("score")).show(20)
// 'The Shawshank Redemption', 'Yojimbo' and 'Harold and Maude' are the three best movies to recommend for user 1
