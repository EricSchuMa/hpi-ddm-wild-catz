package discovery

object testScript extends App {

  override def main(args: Array[String]): Unit = {
    //------------------------------------------------------------------------------------------------------------------
    // Lamda basics (for Scala)
    //------------------------------------------------------------------------------------------------------------------

    //spark uses user defined functions to transform data, lets first look at how functions are defined in scala:
    val smallListOfNumbers = List(1, 2, 3, 4, 5)

    // A Scala map function from int to double
    def squareAndAdd(i: Int): Double = {
      i * 2 + 0.5
    }

    // A Scala map function defined in-line (without curly brackets)
    def squareAndAdd2(i: Int): Double = i * 2 + 0.5

    // A Scala map function inferring the return type
    def squareAndAdd3(i: Int) = i * 2 + 0.5

    // An anonymous Scala map function assigned to a variable
    val squareAndAddFunction = (i: Int) => i * 2 + 0.5

    println(smallListOfNumbers.map(squareAndAdd))
    println(smallListOfNumbers.map(squareAndAdd2))

  }
}
