// import implicits ...??

// https://grouplens.org/datasets/movielens/ --> ml-latest-small
// Data collected from the MovieLens (web-based recommender system)

// --> depuis spark-2.3.4-bin-hadoop2.7/bin:

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
// Question: why +1 in the denominator??
// grade_formula_2 --> similar to the previous formula, but we use the logarithm in order to penalise movies
// with few grades and harmonize grades of those with many grades
// df_ratings_3.map(row => (row.getAs[String]("movieId"),(row.getAs[Float]("rating"),1))).show(5)
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

// Similarity
// Function to get movies rated by x and y
def moviesInter(df_ratings: DataFrame, user_x: String, user_y: String): DataFrame = {
    val df_ratings_x=df_ratings.filter($"userId"===user_x).select($"movieId")
    val df_ratings_y=df_ratings.filter($"userId"===user_y).select($"movieId")
    
    val df_movies_inter=df_ratings_y.intersect(df_ratings_x) // movies intersection

    val df_ratings_x_filtered=df_ratings.filter($"userId"===user_x).filter($"movieId".isin(df_movies_inter.select($"movieId").collect.map(_(0)).toList:_*)).toDF
    val df_ratings_y_filtered=df_ratings.filter($"userId"===user_y).filter($"movieId".isin(df_movies_inter.select($"movieId").collect.map(_(0)).toList:_*)).toDF
    val df_x=df_ratings_x_filtered.withColumnRenamed("rating","rating" + user_x).drop("timestamp").drop("userId")
    val df_y=df_ratings_y_filtered.withColumnRenamed("rating","rating" + user_y).drop("timestamp").drop("userId")
    df_x.join(df_y, Seq("movieId"))
}

def simil(df_inter: DataFrame): Double = { // simil(list lx, list ly)
    var error=false
    var res = 0.0
    try {
        val col_name_x=df_inter.select(df_inter.columns.slice(1,2).map(name=>col(name)):_*).columns.take(1)(0)
        val col_name_y=df_inter.select(df_inter.columns.slice(2,3).map(name=>col(name)):_*).columns.take(1)(0)
        val corr_x_y = df_inter.groupBy().agg(corr(col_name_x, col_name_y)).collect().take(1)(0) // Note: negative correlation indicates that when one variable increases, the other one decreases
        if(corr_x_y.get(0) != null) {
            res=corr_x_y.getDouble(0) * Math.log(1+df_inter.count) // (base exp logarithm)
        }
    } catch {
        case x: AnalysisException => error=true // in case two columns have same name, an error is raised
    }
    res
}


// !!! df_ratings_4.select("userId").dropDuplicates.map(user => simil(moviesInter(df_ratings_4,user.getString(0),"1"))).show(5) !!!
// --> renvoie un NPE car on ne peut pas utiliser un DF dans une transformation (ici un map)
// --> raison: les opérations d'un DF se font sur le master alors qu'une UDF utilise les workers


// Solution: utiliser une autre structure de données que le DataFrame
// => S'aider de la correction du prof: commencer avec un groupByKey: 
val rdd_ratings=df_ratings_4.map(row => (row.getString(0),(row.getString(1),row.getFloat(2)))).rdd.groupByKey
val user1 = rdd_ratings.lookup("1")

def simil2(movieRating1:Seq[Iterable[(String, Float)]], movieRating2:Seq[Iterable[(String, Float)]]):Double = {
    val movieRating1:Map[String,Float] =  movieRating1.map(iter => iter.toMap.map(user => ((user._1),user._2))).take(1)(0) // erreur de type
    return 0.0
}




// Solution SEB-like
// Add column of tuples

val dfRatingList = df_ratings_4.groupBy("userId").agg(collect_list("movieId").alias("userMovies"),collect_list("rating").alias("userRatings"))

def movieToRating(warr1: WrappedArray[Any],warr2: WrappedArray[Any]): Map[String, Float] = {
    val arr1 = warr1.asInstanceOf[WrappedArray[String]].toArray  
    val arr2 = warr2.asInstanceOf[WrappedArray[Float]].toArray
    (arr1 zip arr2).toMap
}
val movieToRatingUdf = udf(movieToRating _)
val dfRatingMap = dfRatingList.withColumn("moviesToRatings",movieToRatingUdf($"userMovies",$"userRatings"))

// Get Map for user 1
val t = dfRatingMap.filter($"userId"==="1").select("moviesToRatings").collect.map(_.toSeq).flatten
val mapUser1 = t(0).asInstanceOf[Map[String, Float]]

