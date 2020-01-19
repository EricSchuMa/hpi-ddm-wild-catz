package discovery

import java.io.File
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.DataType
import scala.collection.mutable

object DependencyDiscovery {
  def main(args: Array[String]): Unit = {
    var data_dir = "./TPCH"
    var cores = "4"

    if (args.length == 2 || args.length == 4) {
      if (args(0) == "--path") data_dir = args(1)
      if (args(0) == "--cores") cores = args(1)
    }
    else if (args.length == 4) {
      if (args(0) == "--path") data_dir = args(1)
      else data_dir = args(3)
      if (args(0) == "--cores") cores = args(1)
      else cores = args(3)
    }
    else if (args.length > 0) {
      println("Invalid arguments!")
      println("Try: java -jar YourAlgorithmName.jar --path TPCH --cores 4")
      System.exit(1)
    }

    printf("\nProgram staring with %s cores...\n", cores)

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    // Create SparkSession
    val sparkBuilder = SparkSession
      .builder()
      .appName("SparkTutorial")
      .master("local[" + cores + "]")
    val spark = sparkBuilder.getOrCreate()

    // Set the default number of shuffle partitions
    spark.conf.set("spark.sql.shuffle.partitions", "8")

    // Importing implicit encoders for standard library classes and tuples that are used as Dataset types
    import spark.implicits._

    // Get all CSV files
    val files = getListOfFiles(data_dir)
    val result = scala.collection.mutable.Map[String, String]()

    files.foreach(file => {
      println("\n==================== Starting with file: " + file + " ====================")
      // Get current file index
      val current_file_index = files.indexOf(file)

      // Read Dataset from the a file
      val df = spark.read
        .option("inferSchema", "true")
        .option("header", "true")
        .option("sep", ";")
        .csv(file)

      // Get column list from Dataset
      val columns = df.columns.toList

      // Iterate through each column of the DataSet
      columns.foreach(column => {
        val current_index = columns.indexOf(column)
        val current_column = column
        println("\n-------------------- Checking for column: " + current_column + " --------------------\n")

        // Get distinct column values
        val setA = df.select(current_column).map(_.get(0).toString).collect.toSet
        val dataTypeSetA = df.schema.fields(current_index).dataType

        // Check inclusion dependency in same file
        if (current_index + 1 < columns.length) {
          // Again iterate through the remaining columns of the Dataset
          for (next_index <- (current_index + 1) until columns.length) {
            val next_column = columns(next_index)

            // Check if the data types of two columns are same
            if (dataTypeSetA == df.schema.fields(next_index).dataType) {
              val setB = df.select(next_column).map(_.get(0).toString).collect.toSet

              // Check if a column is a subset of the other one
              println(current_column + " ⊆ " + next_column + " = " + setA.subsetOf(setB))
              println(next_column + " ⊆ " + current_column + " = " + setB.subsetOf(setA))

              if (setA.subsetOf(setB)) {
                if (result.contains(next_column)) {
                  result(next_column) = result(next_column) + ", " + current_column
                }
                else result += (next_column -> current_column)
              }
              if (setB.subsetOf(setA)) {
                if (result.contains(current_column)) {
                  result(current_column) = result(current_column) + ", " + next_column
                }
                else result += (current_column -> next_column)
              }
            }
          }
        }

        // Check inclusion dependency in other files
        findSubsetInOtherFiles(setA, dataTypeSetA, current_column, files, current_file_index, result, spark)
      })
    })

    println("\nResult:")
    for ((k, v) <- result) printf("%s < %s\n", k, v)
  }

  def findSubsetInOtherFiles(setA: Set[String], dataTypeSetA: DataType, current_column: String, files: List[String],
                             current_file_index: Int, result: mutable.Map[String, String], spark: SparkSession): Unit = {
    // Importing implicit encoders for standard library classes and tuples that are used as Dataset types
    import spark.implicits._

    for (next_file_index <- (current_file_index + 1) until files.length) {
      val next_file = files(next_file_index)
      println("-------------------- Checking in file: " + next_file + " --------------------")

      // Read Dataset from the a file
      val df = spark.read
        .option("inferSchema", "true")
        .option("header", "true")
        .option("sep", ";")
        .csv(next_file)

      // Get column list from Dataset
      val columns = df.columns.toList

      columns.foreach(column => {
        val next_index = columns.indexOf(column)

        if (dataTypeSetA == df.schema.fields(next_index).dataType) {
          // Get distinct column values
          val setB = df.select(column).map(_.get(0).toString).collect.toSet

          // Check if a column is a subset of the other one
          println(current_column + " ⊆ " + column + " = " + setA.subsetOf(setB))
          println(column + " ⊆ " + current_column + " = " + setB.subsetOf(setA))

          if (setA.subsetOf(setB)) {
            if (result.contains(column)) {
              result(column) = result(column) + ", " + current_column
            }
            else result += (column -> current_column)
          }
          if (setB.subsetOf(setA)) {
            if (result.contains(current_column)) {
              result(current_column) = result(current_column) + ", " + column
            }
            else result += (current_column -> column)
          }
        }
      })
    }
  }

  def getListOfFiles(dir: String): List[String] = {
    val file = new File(dir)
    file.listFiles.filter(_.isFile)
      .map(_.getPath).toList
  }
}
