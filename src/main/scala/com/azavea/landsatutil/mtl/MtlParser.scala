package com.azavea.landsatutil.mtl

import org.joda.time.{ DateTime, LocalDate }
import scala.util.parsing.combinator._
import java.io._


class MtlParser extends JavaTokenParsers {

  def date : Parser[Any] =
    """\d{4}-\d{2}-\d{2}""".r map LocalDate.parse

  def dateTime: Parser[Any] =
    """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""".r map { dt =>
      DateTime.parse(dt)
    }

  def float: Parser[Double] =
    ("""[-+]?[0-9]*\.[0-9]+([eE][-+]?[0-9]+)?""".r | """[-+]?[0-9]*\.[0-9]+""".r) map (_.toDouble)

  def int: Parser[Int] =
    """[-+]?[0-9]+""".r map (_.toInt)

  def str: Parser[String] =
    stringLiteral map { s => s.slice(1, s.length - 1) }

  def value : Parser[Any] =
    dateTime | date | float | int | str

  def field: Parser[(String, Any)] =
    ident ~ "=" ~ value map { case (name ~ _ ~ value) => (name, value) }

  def groupStart: Parser[String] =
    "GROUP" ~ "=" ~> ident

  def groupEnd: Parser[String] =
    "END_GROUP" ~ "=" ~> ident

  def mtlGroup: Parser[MtlGroup] =
    groupStart ~ (field*) <~ groupEnd map { case name ~ fields =>
      new MtlGroup(name, fields.toMap)
    }

  def mtlFile: Parser[Map[String, MtlGroup]] =
    (groupStart ~ (mtlGroup*) <~ groupEnd map { case name ~ groups =>
      // discaring the root group name
      (for (group <- groups) yield (group.name, group)).toMap
    })

  def apply(input: Reader) = {
    val res = parseAll(mtlFile <~ "END", input)
    res match {
      case Success(mtl, _) =>
        Some(new MTL(mtl))
      case Failure(msg, _) =>
        println(res)
        None
      case Error(msg, _) =>
        println(res)
        None
    }
   }
}

object MtlParser {
  def parseFile(file: String): Option[MTL] = {
    val parser = new MtlParser()
    parser(new FileReader(file))
  }

  def parseStream(stream: InputStream): Option[MTL] = {
    val parser = new MtlParser()
    try {
      val reader = new BufferedReader(new InputStreamReader(stream))
      parser(reader)
    } finally { stream.close }
  }
}
