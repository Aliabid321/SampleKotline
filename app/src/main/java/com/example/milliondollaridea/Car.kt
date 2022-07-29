package com.example.milliondollaridea

data class Car(val name:String,val model:String,val year:Int)
{
    fun derive(): Car {
      return Car(name,model,year)
    }
}
