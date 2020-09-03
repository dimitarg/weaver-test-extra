package com.dimitarg.example.sharedres

final case class SharedResource(foo: FooResource, bar: BarResource)
final case class FooResource()
final case class BarResource(value: Int)
