package me.pseudoapp

data class Instruction(
    var text: String,
    val type: Type,
    val position: Int,
    val inCondition:Boolean = false
) {
    enum class Type {
        NameValue, //  anything = a
        ResetValue, // {1+1} => a
        StartCondition, // a < b ?
        EndCondition // .
    }


}