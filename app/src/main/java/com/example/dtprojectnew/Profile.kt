package com.example.dtprojectnew

//Profile Exceptions
open class ProfileExceptions(profile: Profile, msg:String) : Exception("Das Profil ${profile.name} mit dem Wert ${profile.value} $msg.")

class ProfileExistsException(profile:Profile) : ProfileExceptions(profile, "existiert")

class ProfileDoesntExistException(profile: Profile) : ProfileExceptions(profile,"existiert nicht")




class Profile(val name:String, val value:Float) {
    //Ich habs jetzt doch anders gelöst mittels it.name == name usw. siehe FileStorage
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Profile) return false

        return ((name == other.name) && (value == other.value))
    }
}
