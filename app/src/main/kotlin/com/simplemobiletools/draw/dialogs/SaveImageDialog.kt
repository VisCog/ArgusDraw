package com.simplemobiletools.draw.dialogs

import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.draw.R
import com.simplemobiletools.draw.R.string.subject
import com.simplemobiletools.draw.activities.SimpleActivity
import com.simplemobiletools.draw.helpers.JPG
import com.simplemobiletools.draw.helpers.PNG
import com.simplemobiletools.draw.helpers.SVG
import com.simplemobiletools.draw.models.Svg
import com.simplemobiletools.draw.views.MyCanvas
import kotlinx.android.synthetic.main.dialog_save_image.*
import kotlinx.android.synthetic.main.dialog_save_image.view.*
import java.io.File
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class SaveImageDialog(var activity: SimpleActivity, val suggestedExtension: String,
                      val curPath: String, var subject: String, var electrode: String,
                      var trial: Int,
                      val canvas: MyCanvas,
                      callback: (path: String, extension: String,
                                 subject: String, electrode: String, trial: Int) -> Unit) {
    private val SIMPLE_DRAW = "Simple Draw"

    init {
        var realPath = if (curPath.isEmpty()) "${activity.internalStoragePath}/$SIMPLE_DRAW" else File(curPath).parent.trimEnd('/')
        val view = activity.layoutInflater.inflate(R.layout.dialog_save_image, null).apply {
            save_image_trial.setText(trial.toString())
            save_image_trial.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(str: Editable) {
                    if (str.isEmpty()) {
                        activity.toast(R.string.trial_cannot_be_empty)
                        return
                    }
                    trial = Integer.parseInt(str.toString())
                    save_image_filename.setText(getFilename())
                }
                override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
            })

            save_image_subject.setText(subject)
            save_image_subject.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(str: Editable) {
                    if (str.isEmpty()) {
                        activity.toast(R.string.subject_cannot_be_empty)
                        return
                    }
                    subject = str.toString()
                    save_image_filename.setText(getFilename())
                }
                override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
            })

            save_image_electrode.setText(electrode)
            save_image_electrode.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(str: Editable) {
                    if (str.isEmpty()) {
                        activity.toast(R.string.electrode_cannot_be_empty)
                        return
                    }
                    electrode = str.toString()
                    save_image_filename.setText(getFilename())
                }
                override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
            })

            save_image_filename.setText(getFilename())
            save_image_radio_group.check(when (suggestedExtension) {
                JPG -> R.id.save_image_radio_jpg
                SVG -> R.id.save_image_radio_svg
                else -> R.id.save_image_radio_png
            })

            save_image_path.text = activity.humanizePath(realPath)
            save_image_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, showFAB = true) {
                    save_image_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this, R.string.save_as) {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val filename = view.save_image_filename.value
                    if (filename.isEmpty()) {
                        activity.toast(R.string.filename_cannot_be_empty)
                        return@setOnClickListener
                    }

                    val extension = when (view.save_image_radio_group.checkedRadioButtonId) {
                        R.id.save_image_radio_png -> PNG
                        R.id.save_image_radio_svg -> SVG
                        else -> JPG
                    }

                    val newFile = File(realPath, "$filename.$extension")
                    if (!newFile.name.isAValidFilename()) {
                        activity.toast(R.string.filename_invalid_characters)
                        return@setOnClickListener
                    }

                    if (saveFile(newFile)) {
                        callback(newFile.absolutePath, extension, subject, electrode, trial)
                        dismiss()
                    } else {
                        activity.toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }

    private fun saveFile(file: File): Boolean {
        if (!file.parentFile.exists()) {
            if (!file.parentFile.mkdir()) {
                return false
            }
        }

        when (file.extension) {
            SVG -> Svg.saveSvg(activity, file, canvas)
            else -> saveImageFile(file)
        }
        activity.scanFile(file) {}
        return true
    }

    private fun saveImageFile(file: File) {
        val msg = file.toString() + " saved successfully.";
        activity.getFileOutputStream(file) {
            writeToOutputStream(file, it!!)
            activity.toast(msg)
        }
    }

    private fun writeToOutputStream(file: File, out: OutputStream) {
        out.use {
            canvas.getBitmap().compress(file.getCompressionFormat(), 70, out)
        }
    }

    private fun getFilename(): String {
        // Display date and time in human readable format:
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH)

        // Add subject ID and electrode name to timestamp
        return "${trial}_${subject}_${electrode}_${sdf.format(Date(System.currentTimeMillis()))}"
    }
}
