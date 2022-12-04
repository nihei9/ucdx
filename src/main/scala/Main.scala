import scala.io.Source

@main def ucdx: Unit = {
  val s = Source.fromURL(
    "https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt"
  )
  UCDParser.parse(s.reader)
  s.close()
}

import java.io.Reader
import scala.util.parsing.combinator.RegexParsers

object UCDParser extends UCD {
  def parse(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}

case class CodePoint(p: Int)

class UCD extends RegexParsers {
  override val whiteSpace = """[ \t\x0B\f\r]""".r
  def ucd: Parser[Any] = rep(line)
  def line: Parser[Any] = (fields ~! opt(comment) | comment) <~! opt("""\n+""".r)
  def comment: Parser[Any] = missingLine | normalComment
  def missingLine: Parser[Any] = "# @missing:" ~>! (codePointRange ~! sepString ~! rep(sepString)) ^^ {
    case cp ~ f ~ rest => (cp, f, rest)
  }
  def normalComment: Parser[String] = """#.*""".r ^^ {
    _.toString
  }
  def fields: Parser[(String, String, String, List[String])] = string ~! sepString ~! sepString ~! rep(sepString) <~! opt("""\n+""".r) ^^ {
    case f1 ~ f2 ~ f3 ~ rest => (f1, f2, f3, rest)
  }
  def string: Parser[String] = """[^\n;#]+""".r ^^ {
    _.toString.trim()
  }
  def sepString: Parser[String] = ";" ~>! string

  def codePointRange: Parser[(CodePoint, CodePoint)] = codePoint ~! opt(".." ~>! codePoint) ^^ {
    case s~None => (s, s)
    case s~Some(e) => (s, e)
  }
  def codePoint: Parser[CodePoint] = """\p{XDigit}+""".r ^^ {
    case s => CodePoint(Integer.parseInt(s, 16))
  }
}
