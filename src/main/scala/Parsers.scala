import java.io.Reader
import scala.util.parsing.combinator.RegexParsers

trait UCDParsers extends RegexParsers {
  override val whiteSpace = """[ \t\x0B\f\r]""".r

  def apply(src: Reader): Either[String, List[Line]] =
    parseAll(ucd, src) match {
      case Success(result, _) => Right(result)
      case NoSuccess(error, _) => Left(error)
    }

  def ucd: Parser[List[Line]] = rep(line)
  def line: Parser[Line] = (fields ~! opt(comment) | comment) <~! opt("""\n+""".r) ^^ {
    case fs ~ c: (UCDElement ~ Option[Option[MissingLine]]) => Line(Some(fs), c.getOrElse(None))
    case c: Option[MissingLine] => Line(None, c)
  }
  def comment: Parser[Option[MissingLine]] = (missingLine | normalComment) ^^ {
    case m: MissingLine => Some(m)
    case _ => None
  }
  def missingLine: Parser[MissingLine] = "# @missing:" ~>! (codePointRange ~! sepString ~! rep(sepString)) ^^ {
    case cp ~ f ~ rest => MissingLine(cp, f, rest)
  }
  def normalComment: Parser[String] = """#.*""".r ^^ {
    _.toString
  }
  def fields: Parser[UCDElement]
  def string: Parser[String] = """[^\n;#]+""".r ^^ {
    _.toString.trim()
  }
  def sepString: Parser[String] = ";" ~>! string
  def sepOptString: Parser[Option[String]] = ";" ~>! opt(string)
  def codePointRange: Parser[CodePointRange] = codePoint ~! opt(".." ~>! codePoint) ^^ {
    case s~None => CodePointRange(s, s)
    case s~Some(e) => CodePointRange(s, e)
  }
  def codePoint: Parser[CodePoint] = """\p{XDigit}+""".r ^^ {
    case s => CodePoint(Integer.parseInt(s, 16))
  }
}

object PropertyValueAliases extends UCDParsers {
  override def fields: Parser[PropertyValueAliasesFields] = string ~! sepString ~! sepString ~! rep(sepString) ^^ {
    case f1 ~ f2 ~ f3 ~ rest =>
      if f1 == "ccc" then PropertyValueAliasesFields(f1, rest.head, f2::f3::rest.tail)
      else PropertyValueAliasesFields(f1, f3, f2::rest)
  }
}

object UnicodeData extends UCDParsers {
  override def fields: Parser[UnicodeDataFields] = codePoint ~! rep(sepOptString) ^^ {
    case cp ~ fs => UnicodeDataFields(cp, fs(1))
  }
}
