package com.example.dtprojectnew

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.dtprojectnew.databinding.ActivityStorageSelectionBinding
import com.google.android.material.button.MaterialButton
import java.io.File




class StorageSelection : AppCompatActivity() {

    private fun getListOfJsonFiles(): List<File> {
        val filesDir = this.filesDir
        val jsonFiles = filesDir.listFiles { _, name -> name.endsWith(".json") }
        return jsonFiles?.toList() ?: emptyList()
    }

    private lateinit var binding: ActivityStorageSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val jsonFiles = getListOfJsonFiles()


        jsonFiles.forEach { file ->
            // Create a horizontal LinearLayout to hold the data button and the delete button
            val horizontalLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16.dpToPx(context)) // Add a 16dp bottom margin
                }
                orientation = LinearLayout.HORIZONTAL
            }

            // Create the data button
            val button = MaterialButton(this).apply {
                text = file.nameWithoutExtension
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    // Create an Intent to navigate to the Storage activity
                    val intent = Intent(this@StorageSelection, Storage::class.java)

                    // Pass the name of the pressed button to the Storage activity
                    intent.putExtra("buttonName", file.nameWithoutExtension)

                    // Start the Storage activity
                    startActivity(intent)
                }
            }

            // Create the delete button with the trashcan symbol
            val deleteButton = ImageButton(this).apply {
                setImageResource(R.drawable.trashcan)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8.dpToPx(context), 0, 0, 0) // Add an 8dp left margin
                }
                background = ContextCompat.getDrawable(context, R.drawable.button_background)
                setOnClickListener {
                    // Create an AlertDialog to confirm the deletion
                    val builder = AlertDialog.Builder(this@StorageSelection)
                    builder.setTitle("Confirm Deletion")
                    builder.setMessage("Are you sure you want to delete this file?")
                    builder.setPositiveButton("Yes") { _, _ ->
                        // Delete the JSON file
                        file.delete()

                        // Remove the button and the delete button from the layout
                        binding.buttonsContainer.removeView(horizontalLayout)
                    }
                    builder.setNegativeButton("No") { dialog, _ ->
                        // Close the AlertDialog without doing anything
                        dialog.dismiss()
                    }
                    builder.create().show()
                }
            }

            // Add the data button and the delete button to the horizontal layout
            horizontalLayout.addView(button)
            horizontalLayout.addView(deleteButton)

            // Add the horizontal layout to the buttons_container
            binding.buttonsContainer.addView(horizontalLayout)
        }



        binding.btnMainPage.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Extension function to convert dp to px
    private fun Int.dpToPx(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return (this * metrics.density).toInt()
    }
}
