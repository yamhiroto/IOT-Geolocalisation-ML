import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import java.io.File

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

val spark = SparkSession
      .builder
      .config(conf)
      .appName("TP Spark : Trainer")
      .getOrCreate()

val training_set = new File("/home/savoga/spark-2.3.4-bin-hadoop2.7/bin/data_kickstarter/prepared_trainingset")

val df = spark.read.parquet(training_set.toString)

// ***** Utilisation des données textuelles *****
// TF-IDF est une technique très utilisée pour représenter l'importance d'un mot dans un corpus de texte
// TF-IDF(t,d,D)
// Le premier terme (TF) mesure l'importance d'un mot dans un texte
// Le deuxième terme (IDF) est utilisé pour compenser le fait qu'un mot peut être présent dans beaucoup de texte de manière générale (donc pas très important)
// Note 1: on utilise le log pour avoir 0 dans le cas où un mot est présent dans tous les documents
// Note 2: un smoothing term est utilisé (+1) pour éviter une division par zéro

// Transformer chaque phrase en une liste de mots
import org.apache.spark.ml.feature.{RegexTokenizer, Tokenizer}
val tokenizer = new RegexTokenizer()
      .setPattern("\\W+")
      .setGaps(true)
      .setInputCol("text")
      .setOutputCol("tokens")

// Retirer les stop words (= les mots les plus courants d'une langue définie)
import org.apache.spark.ml.feature.StopWordsRemover
val remover = new StopWordsRemover()
      .setInputCol("tokens")
      .setOutputCol("tokens_filtered")

// Calculer la partie TF
// e.g. (xxx,[aa,bb,cc],[1.0,2.0,1.0])
// xxx est le nombre total de mots dans le corpus de textes
// le premier vecteur est l'indice du mot dans le corpus
// le second vecteur est la fréquence du mot dans le document
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel}

val cvModel: CountVectorizer = new CountVectorizer()
      .setInputCol("tokens_filtered")
      .setOutputCol("TF")

// Calculer la partie IDF => donne le score TFIDF
// e.g. (xxx,[aa,bb,cc],[3.75,2.02,4.0])
// le second vecteur est le score TFIDF du mot
// Note: un score élevé est lorsqu'un mot est fréquent dans un document mais peu fréquent dans tous les docs
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
val idf = new IDF()
      .setInputCol("TF")
      .setOutputCol("tfidf")

// ***** Conversion des variables catégorielles en variables numériques *****

// Convertir country2 en quantités numériques
import org.apache.spark.ml.feature.StringIndexer
val indexer = new StringIndexer()
      .setInputCol("country2")
      .setOutputCol("country_indexed")

// Convertir currency2 en quantités numériques
val indexer_2 = new StringIndexer()
      .setInputCol("currency2")
      .setOutputCol("currency_indexed")

// One-Hot encoder ces 2 catégories
// One-Hot encoding: transformer une valeur en un vecteur avec que des 0 et un seul 1
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer}

val encoder = new OneHotEncoder()
      .setInputCol("country_indexed")
      .setOutputCol("country_onehot")

val encoder_2 = new OneHotEncoder()
      .setInputCol("currency_indexed")
      .setOutputCol("currency_onehot")

// ***** Mettre les données sous une forme utilisable par SparkML *****

// Assembler toutes les features dans un unique vecteur
import org.apache.spark.ml.feature.VectorAssembler

val assembler = new VectorAssembler()
      .setInputCols(Array("tfidf", "days_campaign", "hours_prepa", "goal", "country_onehot", "currency_onehot"))
      .setOutputCol("features")

// Créer/Instancier le modèle de classification
import org.apache.spark.ml.classification.LogisticRegression

val lr = new LogisticRegression()
      .setElasticNetParam(0.0)
      .setFitIntercept(true)
      .setFeaturesCol("features")
      .setLabelCol("final_status")
      .setStandardization(true)
      .setPredictionCol("predictions")
      .setRawPredictionCol("raw_predictions")
      .setThresholds(Array(0.7, 0.3))
      .setTol(1.0e-6)
      .setMaxIter(20)

// Création du pipeline
import org.apache.spark.ml.{Pipeline, PipelineModel}
val pipeline = new Pipeline()
      .setStages(Array(tokenizer, remover, cvModel, idf, indexer, indexer_2, encoder, encoder_2, assembler, lr))

// ***** Entraînement et test du modèle *****

import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator

val evaluator = new MulticlassClassificationEvaluator()
      .setMetricName("f1")
      .setLabelCol("final_status")
      .setPredictionCol("predictions")

// ***** Réglage des hyper-paramètres (a.k.a. tuning) du modèle *****

// Split des données en training et test sets
val splits = df.randomSplit(Array(0.9, 0.1), seed = 11L)
val dfTraining = splits(0)
val dfTest = splits(1)

// Grid search
import org.apache.spark.ml.tuning.ParamGridBuilder

val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(math.pow(10, -8), math.pow(10, -6), math.pow(10, -4), math.pow(10, -2)))
      .addGrid(cvModel.minDF, (55.0 to 95.0 by 20).toArray)
      .build()

import org.apache.spark.ml.tuning.TrainValidationSplit

val tvs = new TrainValidationSplit()
      .setEstimator(pipeline)
      .setEvaluator(evaluator)
      .setEstimatorParamMaps(paramGrid)
      .setTrainRatio(0.9)

val model = tvs.fit(dfTraining)
val dfWithPredictions = model.transform(dfTest).select("features", "final_status", "predictions")
val f1_score = evaluator.evaluate(dfWithPredictions)

println(s"f-score après grid search: ${f1_score}")

dfWithPredictions.groupBy("final_status", "predictions").count.show()
//model.write.overwrite().save("src/main/resources/output_model")

