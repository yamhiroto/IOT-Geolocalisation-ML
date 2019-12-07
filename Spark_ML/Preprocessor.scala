import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

val conf = new SparkConf().setAll(Map(
      "spark.scheduler.mode" -> "FIFO",
      "spark.speculation" -> "false",
      "spark.reducer.maxSizeInFlight" -> "48m",
      "spark.serializer" -> "org.apache.spark.serializer.KryoSerializer",
      "spark.kryoserializer.buffer.max" -> "1g",
      "spark.shuffle.file.buffer" -> "32k",
      "spark.default.parallelism" -> "12",
      "spark.sql.shuffle.partitions" -> "12"
    ))

// Le SparkSession permet d'utiliser les DataFrames
val spark = SparkSession
      .builder
      .config(conf)
      .appName("TP Spark : Preprocessor")
      .getOrCreate()

import spark.implicits._

// ***** Chargement des données ***** 
val df: DataFrame = spark
  .read
  .option("header", true) // utilise la première ligne du (des) fichier(s) comme header
  .option("inferSchema", "true") // pour inférer le type de chaque colonne (Int, String, etc.)
  .csv("data_kickstarter/train_clean.csv")

// Affichage des données
println(s"Nombre de lignes : ${df.count}")
println(s"Nombre de colonnes : ${df.columns.length}")

df.show(5)

// Nom de chaque colonne avec son type
df.printSchema()

// Caster les colonnes qui semblent contenir des entiers
 val dfCasted: DataFrame = df
      .withColumn("goal", $"goal".cast("Int"))
      .withColumn("deadline", $"deadline".cast("Int"))
      .withColumn("state_changed_at", $"state_changed_at".cast("Int"))
      .withColumn("created_at", $"created_at".cast("Int"))
      .withColumn("launched_at", $"launched_at".cast("Int"))
      .withColumn("backers_count", $"backers_count".cast("Int"))
      .withColumn("final_status", $"final_status".cast("Int"))

dfCasted.printSchema()

// ***** Cleaning *****
// Afficher une description statistique des colonnes de type Int
dfCasted
      .select("goal","backers_count","final_status")
      .describe()
      .show

// Repérage du cleaning à faire
dfCasted.groupBy("disable_communication").count.orderBy($"count".desc).show(10)
dfCasted.groupBy("country").count.orderBy($"count".desc).show(10)
dfCasted.groupBy("currency").count.orderBy($"count".desc).show(10)
dfCasted.groupBy("state_changed_at").count.orderBy($"count".desc).show(10)
dfCasted.groupBy("backers_count").count.orderBy($"count".desc).show(10)
dfCasted.select("goal", "final_status").show(10)
dfCasted.groupBy("country", "currency").count.orderBy($"count".desc).show(10)
dfCasted.select("deadline").dropDuplicates.show(10)

// Suppression d'une colonne majoritairement à false
val dfDeletedFalseCol: DataFrame = dfCasted.drop("disable_communication")

// On enlève les colonnes susceptibles de générer des fuites du futur
val dfNoFutur: DataFrame = dfDeletedFalseCol.drop("backers_count", "state_changed_at") // le nombre d'investisseurs n'est connu qu'à la fin de la campagne

// Affichage de la colonne 'country' qui comporte des typo
df.filter($"country" === "False")
      .groupBy("currency")
      .count
      .orderBy($"count".desc)
      .show(50)

// Création de deux UDFs pour corriger le problème de la colonne 'country'
def cleanCountry(country: String, currency: String): String = {
  if (country == "False")
    currency
  else
    country
}

def cleanCurrency(currency: String): String = {
  if (currency != null && currency.length != 3)
    null
  else
    currency
}

val cleanCountryUdf = udf(cleanCountry _)
val cleanCurrencyUdf = udf(cleanCurrency _)

val dfCorrectedCountry: DataFrame = dfNoFutur
  .withColumn("country2", cleanCountryUdf($"country", $"currency"))
  .withColumn("currency2", cleanCurrencyUdf($"currency"))
  .drop("country", "currency")

// Afficher le nombre d'éléments de chaque classe
dfCorrectedCountry.filter($"final_status" === 0).count
dfCorrectedCountry.filter($"final_status" === 1).count

// Supprimer les classes autre que 0 et 1
def isZeroOrOne(status: Integer): Boolean = {
  if (status == 0 | status == 1)
    true
  else
    false
}
val isZeroOrOneUdf = udf(isZeroOrOne _)
val dfCleanedOutput = dfCorrectedCountry
  .withColumn("final_status2", isZeroOrOneUdf($"final_status"))
  .filter($"final_status2" === true)
  .drop("final_status2")

dfCleanedOutput.filter($"final_status" !== 0).filter($"final_status" !== 1).show(5) // vérification de la suppression

// ***** Ajouter et manipuler des colonnes *****
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.from_unixtime

// Ajout de la durée de la campagne en jours
val dfDaysCampaign: DataFrame = dfCleanedOutput.withColumn("days_campaign", datediff(from_unixtime($"deadline"),from_unixtime($"launched_at")))

// Ajout du nombre d’heures de préparation de la campagne
val dfHoursPrepa: DataFrame = dfDaysCampaign.withColumn("hours_prepa", round(($"launched_at"-$"created_at")/3600.floatValue(),3))

// Suppression des colonnes non exploitables
val dfDeletedCol: DataFrame = dfHoursPrepa.drop("launched_at","deadline","created_at")

// Colonnes en minuscules
val dfLowerCol: DataFrame = dfDeletedCol.withColumn("name",lower($"name"))
    .withColumn("desc",lower($"desc"))
    .withColumn("keywords",lower($"keywords"))

// Concaténation
val dfConcat: DataFrame = dfLowerCol.withColumn("text_temp", concat($"name", lit(" "), $"desc"))
      .withColumn("text", concat($"text_temp", lit(" "), $"keywords"))
      .drop($"text_temp")

// Gestion des valeurs nulles
val map = Map("days_campaign" -> -1, "hours_prepa" -> -1, "goal" -> -1, "country2" -> "unknown", "currency2" -> "unknown")
val dfReplacedNull: DataFrame = dfConcat.na.fill(map)

// ***** Enregistrer les données nettoyées en format parquet *****
// ATTENTION: le dossier DF ne doit pas déjà exister...
import java.io.File
val data_cleaned = new File("/home/savoga/spark-2.3.4-bin-hadoop2.7/bin/data_kickstarter/DF")
dfReplacedNull.write.parquet(data_cleaned.toString)
