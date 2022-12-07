import org.json4s.JValue
import org.json4s.JsonDSL._

trait UCDElement {
  def serialize: JValue
}

case class CodePoint(p: Int) extends UCDElement {
  def serialize: JValue = p
}

case class CodePointRange(
  start: CodePoint,
  end: CodePoint
) extends UCDElement {
  def serialize: JValue = Map(
    "s" -> start.serialize,
    "e" -> end.serialize
  )
}

case class MissingLine(
  cpRange: CodePointRange,
  field: String,
  rest: List[String]
) extends UCDElement {
  def serialize: JValue = ("cp" -> cpRange.serialize) ~ ("default" -> (field::rest))
}

case class Line(
  fields: Option[UCDElement],
  missingLine: Option[MissingLine]
) extends UCDElement {
  def serialize: JValue = {
    ("fields" ->
      (fields match
        case Some(fs) => Some(fs.serialize)
        case None => None))
    ~ ("missingLine" ->
      (missingLine match
        case Some(ml) => Some(ml.serialize)
        case None => None))
  }
}

case class PropertyValueAliasesFields(
  propertyName: String,
  propertyValueNameFormal: String,
  propertyValueNameAliases: List[String],
) extends UCDElement {
  def serialize: JValue = {
    ("name" -> propertyName)
    ~ ("formal" -> propertyValueNameFormal)
    ~ ("aliases" -> propertyValueNameAliases)
  }
}

case class UnicodeDataFields(
  codePoint: CodePoint,
  generalCategory: Option[String]
) extends UCDElement {
  def serialize: JValue = {
    ("cp" -> codePoint.serialize)
    ~ ("gc" -> generalCategory)
  }
}
