import scala.io.Source

@main def ucdx: Unit = {
  val propValAliases = Source.fromURL("https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt")
  PropertyValueAliases(propValAliases.reader)
  propValAliases.close()

  val uniData = Source.fromURL("https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt")
  UnicodeData(uniData.reader)
  uniData.close()
}

import java.io.Reader
import scala.util.parsing.combinator.RegexParsers

case class CodePoint(p: Int)
case class CodePointRange(start: CodePoint, end: CodePoint)
case class MissingLine(cpRange: CodePointRange, field: String, rest: List[String])

trait UCDParsers[A] extends RegexParsers {
  override val whiteSpace = """[ \t\x0B\f\r]""".r

  type Line = (Option[A], Option[MissingLine])

  def ucd: Parser[List[Line]] = rep(line)
  def line: Parser[Line] = ((fields ~! opt(comment) | comment) <~! opt("""\n+""".r)) ^^ {
    case fs ~ c: (A ~ Option[MissingLine]) => (Some(fs), c)
    case c: Option[MissingLine] => (None, c)
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
  def fields: Parser[A]
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

type PropertyValueAliasesFields = (String, String, String, List[String])

object PropertyValueAliases extends UCDParsers[PropertyValueAliasesFields] {
  override def fields: Parser[PropertyValueAliasesFields] = string ~! sepString ~! sepString ~! rep(sepString) ^^ {
    case f1 ~ f2 ~ f3 ~ rest => (f1, f2, f3, rest)
  }
  def apply(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}

type UnicodeDataFields = (CodePoint, List[Option[String]])

object UnicodeData extends UCDParsers[UnicodeDataFields] {
  override def fields: Parser[UnicodeDataFields] = codePoint ~! rep(sepOptString) ^^ {
    case cp ~ fs => (cp, fs)
  }
  def apply(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}
