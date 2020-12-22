import java.util.regex.Pattern

object FlagTest
{
    private val tester = "--([a-zA-Z]+)(=([^\"'\\s]+|\"((?:[^\"\\\\]|\\\\.)*)\"|'((?:[^'\\\\]|\\\\.)*)'))?"

    @JvmStatic
    fun main(args: Array<String>)
    {
        val inp = "-time zones --testt=\"bruh\" --br=2 --t"
        val sb = StringBuilder()

        val flags = mutableMapOf<String, String?>()
                .apply {
                    var index = 0
                    val matcher = Pattern.compile(tester).matcher(inp)

                    while (matcher.find())
                    {
                        put(matcher.group(1), matcher.group(5) ?: matcher.group(3))
                        sb.append(inp.substring(index, matcher.start()))
                        index = matcher.end()
                    }
                    if(index < inp.length)
                        sb.append(inp.substring(index, inp.length))
                }

        println(sb.toString())

        println(flags)
    }
}