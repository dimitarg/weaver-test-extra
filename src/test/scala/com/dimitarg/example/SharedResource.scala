package com.dimitarg.example

final case class SharedResource(foo: FooResource, bar: BarResource)
final case class FooResource()
final case class BarResource(value: Int)
