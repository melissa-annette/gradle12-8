package com.h0tk3y.kotlin.staticObjectNotation.parsing

import com.h0tk3y.kotlin.staticObjectNotation.language.Block
import com.h0tk3y.kotlin.staticObjectNotation.language.Import
import com.h0tk3y.kotlin.staticObjectNotation.language.LanguageTreeElement
import com.h0tk3y.kotlin.staticObjectNotation.language.SourceData

data class LanguageTreeResult(
    val imports: List<Import>,
    val topLevelBlock: Block,
    val headerFailures: List<SingleFailureResult>,
    val codeFailures: List<SingleFailureResult>
) {
    val allFailures = headerFailures + codeFailures
}

sealed interface LanguageResult<out T>

sealed interface ElementResult<out T : LanguageTreeElement> : LanguageResult<T> {
    fun <R : LanguageTreeElement> flatMap(mapping: (T) -> ElementResult<R>): ElementResult<R> = when (this) {
        is Element -> mapping(element)
        is FailingResult -> this
    }
}

sealed interface SyntacticResult<out T> : LanguageResult<T> {
    fun <R> flatMap(mapping: (T) -> SyntacticResult<R>): SyntacticResult<R> = when (this) {
        is Syntactic -> mapping(value)
        is FailingResult -> this
    }
}

data class Element<T : LanguageTreeElement>(val element: T) : ElementResult<T>
data class Syntactic<out T>(val value: T) : SyntacticResult<T>
sealed interface FailingResult : ElementResult<Nothing>, SyntacticResult<Nothing>
sealed interface SingleFailureResult : FailingResult

data class UnsupportedConstruct(
    val potentialElementSource: SourceData,
    val erroneousSource: SourceData,
    val languageFeature: UnsupportedLanguageFeature
) : SingleFailureResult

data class ParsingError(
    val potentialElementSource: SourceData,
    val erroneousSource: SourceData,
    val message: String
) : SingleFailureResult

data class MultipleFailuresResult(val failures: List<SingleFailureResult>) :
    FailingResult // TODO: should this exist at all?

sealed interface UnsupportedLanguageFeature {
    data object PackageHeader : UnsupportedLanguageFeature
    data object TypeDeclaration : UnsupportedLanguageFeature
    data object CollectionLiteral : UnsupportedLanguageFeature
    data object SupertypeUsage : UnsupportedLanguageFeature
    data object FunctionDeclaration : UnsupportedLanguageFeature
    data object LabelledStatement : UnsupportedLanguageFeature
    data object ExplicitVariableType : UnsupportedLanguageFeature
    data object StarImport : UnsupportedLanguageFeature
    data object RenamingImport : UnsupportedLanguageFeature
    data object AnnotationUsage : UnsupportedLanguageFeature
    data object LoopStatement : UnsupportedLanguageFeature
    data object ConditionalExpression : UnsupportedLanguageFeature
    data object ControlFlow : UnsupportedLanguageFeature
    data object LambdaWithParameters : UnsupportedLanguageFeature
    data object AugmentingAssignment : UnsupportedLanguageFeature
    data object UnsupportedLiteral : UnsupportedLanguageFeature
    data object LocalVarNotSupported : UnsupportedLanguageFeature
    data object ExtensionProperty : UnsupportedLanguageFeature
    data object DelegatedProperty : UnsupportedLanguageFeature
    data object CustomAccessor : UnsupportedLanguageFeature
    data object UninitializedProperty : UnsupportedLanguageFeature
    data object MultiVariable : UnsupportedLanguageFeature
    data object GenericDeclaration : UnsupportedLanguageFeature
    data object CallableReference : UnsupportedLanguageFeature
    data object UnsupportedOperator : UnsupportedLanguageFeature
    data object UnsupportedShebangInScript : UnsupportedLanguageFeature
    data object InvokeOperator : UnsupportedLanguageFeature
    data object GenericExpression : UnsupportedLanguageFeature
    data object ValModifierNotSupported : UnsupportedLanguageFeature
    data object Indexing : UnsupportedLanguageFeature
    data object InvalidLanguageConstruct : UnsupportedLanguageFeature
    data object PrefixExpression : UnsupportedLanguageFeature
    data object UnsupportedOperationInBinaryExpression: UnsupportedLanguageFeature

    data object SafeNavigation : UnsupportedLanguageFeature

    data object Reflection : UnsupportedLanguageFeature
    data object TodoNotCoveredYet : UnsupportedLanguageFeature
}
