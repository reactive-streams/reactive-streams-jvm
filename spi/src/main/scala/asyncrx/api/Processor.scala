package asyncrx.api

/**
 * A Processor is a stand-alone representation of a transformation for
 * elements from In to Out types. Implementations of this API will provide
 * factory methods for creating Processors and connecting them to
 * [[Producer]] and [[Consumer]].
 */
trait Processor[In, Out] extends Consumer[In] with Producer[Out]
