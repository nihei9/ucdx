import scala.io.Source

@main def ucdx: Unit = {
  val propValAliases = Source.fromURL("https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt")
  PropertyValueAliases(propValAliases.reader)
  propValAliases.close()

  val uniData = Source.fromURL("https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt")
  UnicodeData(uniData.reader)
  uniData.close()
}
