package RW.ToIgnite

import jpoint.titanic.TitanicUtils
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.sql.SparkSession

/**
  * Predict surviving based on integer data
  * <p>
  * The main problem are nulls in data. Values to assemble (by VectorAssembler) cannot be null.
  */
object Ex_2_LinReg {
    def main(args: Array[String]): Unit = {

        val spark: SparkSession = TitanicUtils.getSparkSession

        val passengers = TitanicUtils.readPassengersWithCasting(spark)
            .select("survived", "pclass", "sibsp", "parch", "sex", "embarked", "age")

        // Step - 1: Make Vectors from dataframe's columns using special Vector Assmebler
        val assembler = new VectorAssembler()
            .setInputCols(Array("pclass", "sibsp", "parch", "survived"))
            .setOutputCol("features")

        // Step - 2: Transform dataframe to vectorized dataframe with dropping rows
        val output = assembler.transform(
            passengers.na.drop(Array("pclass", "sibsp", "parch", "survived", "age")) // <============== drop row if it has nulls/NaNs in the next list of columns
        ).select("features", "age")

        val lr = new LinearRegression()
            .setMaxIter(100)
            .setRegParam(0.1)
            .setElasticNetParam(0.1)
            .setLabelCol("age")
            .setFeaturesCol("features")

        // Fit the model
        val model = lr.fit(output)
        model.write.overwrite().save("/home/zaleslaw/models/titanic/linreg")
        // Print the coefficients and intercept for linear regression
        println(s"Coefficients: ${model.coefficients} Intercept: ${model.intercept}")

        // Summarize the model over the training set and print out some metrics
        val trainingSummary = model.summary
        println(s"numIterations: ${trainingSummary.totalIterations}")
        println(s"objectiveHistory: [${trainingSummary.objectiveHistory.mkString(",")}]")
        trainingSummary.residuals.show()
        println(s"RMSE: ${trainingSummary.rootMeanSquaredError}")
        println(s"r2: ${trainingSummary.r2}")


        // Step - 5: Predict with the model
        val rawPredictions = model.transform(output)
        rawPredictions.show(2000)

        // Step - 6: Evaluate prediction
        val evaluator = new RegressionEvaluator()
            .setLabelCol("age")
            .setPredictionCol("prediction")
            .setMetricName("mse")

        // Step - 7: Calculate accuracy
        val mse = evaluator.evaluate(rawPredictions)
        println("MSE = " + mse)


    }
}