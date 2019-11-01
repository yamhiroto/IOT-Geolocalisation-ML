// import implicits ...??

// https://grouplens.org/datasets/movielens/ --> ml-latest-small
// Data collected from the MovieLens (web-based recommender system)

// --> depuis spark-2.3.4-bin-hadoop2.7/bin:

// Import data
val df_ratings = spark.read.option("header", false).csv("ml-latest-small/ratings.csv")
val df_movies = spark.read.option("header", false).csv("ml-latest-small/movies.csv")

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

// Better ordered list
// grade_formula_1 --> the numerator is increasing higher that the denominator when the number of grades increases
// => it allows to penalise the movies that has only few grades
// Question: why +1 in the denominator??
// grade_formula_2 --> similar to the previous formula, but we use the logarithm in order to penalise movies
// with few grades and harmonize grades of those with many grades
// ***MAP***
df_ratings_3.map(row => (row.getAs[String]("_c1"),(row.getAs[Float]("_c2"),1))).show(5)
// ***REDUCEBYKEY***

