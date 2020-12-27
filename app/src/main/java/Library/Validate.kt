package Library

import java.util.regex.Matcher
import java.util.regex.Pattern

class Validate {
    companion object{
        var pat : Pattern? = null
        var mat : Matcher? = null

        fun isEmail(email : String) : Boolean{
            pat = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+")
            mat = pat!!.matcher(email)
            return mat!!.find()
        }
    }
}