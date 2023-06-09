package com.example.dtprojectnew

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ProfileStorage(private val context: Context) {
    private val fileName = "profiles.json"
    private val folderName = "Profiles"

    fun saveProfile(profile: Profile) {
        val profiles = loadProfiles().toMutableList()
        if(is_inProfile(profiles, profile))
            throw(ProfileExistsException(profile))
        profiles.add(profile)
        saveProfiles(profiles)
    }

    fun is_inProfile(profiles:MutableList<Profile>,profile:Profile):Boolean{
        return profiles.any { it == profile }
    }
    fun saveProfiles(profiles: List<Profile>) {
        val json = Gson().toJson(profiles)
        val folder = File(context.filesDir, folderName)
        val file = File(folder, fileName)

        //Erstelle den Ordner falls noch nicht existent
        if (!folder.exists())
            folder.mkdir()

        file.writeText(json)
    }

    fun loadProfiles(): List<Profile> {
        val folder = File(context.filesDir, folderName)
        val file = File(folder, fileName)

        if (!file.exists())
            return emptyList()


        val json = file.readText()
        val typeToken = object : TypeToken<List<Profile>>() {}.type
        return Gson().fromJson(json, typeToken)
    }

    fun clearJsonFile() {
        val file = File(context.filesDir, "profiles.json")
        file.delete()
    }

    fun deleteProfile(profile: Profile) {
        val profiles = loadProfiles().toMutableList()
        //Wir nehmen name und value als Schlüssel, sonst müsste ich die equals funtion in Profile überschreiben sollte so nicht nötig sein
        if(!profiles.remove(profile)){
            throw(ProfileDoesntExistException(profile))
        }

        saveProfiles(profiles)
    }

}