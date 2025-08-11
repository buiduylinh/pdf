package com.example.pdfsigner

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doAfterTextChanged
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.util.Constants
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_show_document.*
import kotlinx.android.synthetic.main.activity_show_document.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class ShowDocumentActivity : AppCompatActivity() {

    var isEditting: Boolean = false
    var x: Float = 0F
    var y: Float = 0F
    var canvas: Canvas? = null

    var margin : Double = 0.0
    var textSize = 30
    var isEditing = false
    var isAddText = false
    var data : FileModel? = null
    var isFirst = true;
    var listEdittext : ArrayList<EditText> = ArrayList()
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_document)
        data = intent.getParcelableExtra<FileModel>("URIPDF")

        Constants.Pinch.MINIMUM_ZOOM = 1F;
        Constants.Pinch.MAXIMUM_ZOOM = 1F;
        btnBack.setOnClickListener {
            finish()
        }
        loadPdfView()

        btnPlus.setOnClickListener {
            if(textSize< 100) {
                textSize = textSize + 1
                tvSizeText.text = textSize.toString()
            }
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat(), resources.displayMetrics);
            signature_pad_real.setTextSize(px.toInt());
            listEdittext.forEach {
                it.textSize = textSize.toFloat()
            }
        }

        btnMinus.setOnClickListener {
            if(textSize > 1) {
                textSize = textSize - 1
                tvSizeText.text = textSize.toString()
            }
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat(), resources.displayMetrics);
            signature_pad_real.setTextSize(px.toInt());
            listEdittext.forEach {
                it.textSize = textSize.toFloat()
            }
        }

        btnDraw.setOnClickListener {
            signature_pad_real.isEditText  = false
            ImageViewCompat.setImageTintList(btnDraw, ColorStateList.valueOf(resources.getColor(R.color.colorSelect)));
            ImageViewCompat.setImageTintList(btnText, null);
            layoutEditing.setOnTouchListener(null)
        }

        btnClear.setOnClickListener {
            signature_pad_real.clear()
            listEdittext.forEach {
                layoutEditing.removeView(it)
            }
            listEdittext.clear()

        }

        btnText.setOnClickListener {
            signature_pad_real.isEditText  = true
            ImageViewCompat.setImageTintList(btnText, ColorStateList.valueOf(resources.getColor(R.color.colorSelect)));
            ImageViewCompat.setImageTintList(btnDraw, null);
            layoutEditing.setOnTouchListener { v, event ->
                val x = event.x.toInt()
                val y = event.y.toInt()
                Log.e("Position", "${x} / ${y}")
                if(event.action == MotionEvent.ACTION_DOWN) {
                    val editText = EditText(btnDraw.context)
                    editText.requestFocus()
                    editText.textSize = textSize.toFloat()

                    val layoutParam = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat(), resources.displayMetrics);
                    editText.doAfterTextChanged {
                        signature_pad_real.drawText(editText.text.toString(), x, y + px.toInt(), textSize)
                    }
                    editText.updatePadding(0, 0, 0, 0)
                    editText.setTextColor(resources.getColor(R.color.transparent))

                    layoutParam.topMargin = y
                    layoutParam.leftMargin = x
                    editText.layoutParams = layoutParam
                    listEdittext.add(editText)
                    layoutEditing.addView(editText)
                }

                true

            }
        }

        btnEdit.setOnClickListener {
            listEdittext.forEach {
                it.clearFocus()
            }
            if (isEditting) {
                Observable.fromCallable {

                    val pdf = PDFTools(applicationContext)
                    pdf.open(data!!.fileUri)
                    val page = pdf.getPDPage(pdfView.currentPage + 1)

                    pdf.getPosition(pdfView.currentPage + 1)
                    val currentPage = pdfView.currentPage + 1
                    val outputSignature = saveSignature(signature_pad_real.getTransparentSignatureBitmap(false)!!)

                    pdf.insertImage(outputSignature, 0, 0,page.mediaBox.width.toInt(), page.mediaBox.height.toInt() * (canvas!!.width.toFloat() / page.mediaBox.width).toInt(), currentPage)

                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = {
                        restoreMode()

                        loadPdfView()
                        hideKeyboard()

                    }, onError = {
                        Log.e("onError", it.message.toString())
                    })
            } else {
               modeEditing()
            }
        }

    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(currentFocus?.windowToken, 0);

    }

    private fun modeEditing() {
        isEditting = true
        btnEdit.setImageResource(R.drawable.ic_save)
        layoutButton.visibility = View.VISIBLE
        layoutEditing.visibility = View.VISIBLE

    }

    private fun restoreMode() {
        isEditting = false
        btnEdit.setImageResource(R.drawable.ic_edit)
        layoutButton.visibility = View.GONE
        signature_pad_real.clear()
        listEdittext.forEach {
            layoutEditing.removeView(it)
        }
        listEdittext.clear()
        layoutEditing.visibility = View.GONE
        hideKeyboard()
    }


    private fun loadPdfView() {
        try {
            pdfView.fromUri(data!!.fileUri).swipeHorizontal(false).onDraw { canvas, pageWidth, pageHeight, displayedPage ->
                this.canvas = canvas
                canvas?.let {
                    if (isFirst) {
                        val pdf = PDFTools(applicationContext)

                        pdf.open(data!!.fileUri)
                        val page = pdf.getPDPage(pdfView.currentPage + 1)
                        val heightNewCanvas = ((page.mediaBox.height /page.mediaBox.width) * it.width).toInt()
                        margin = (canvas.height - heightNewCanvas).toDouble() / 2

                        isFirst = false
                        val layoutParam = FrameLayout.LayoutParams(
                            it.width,
                            heightNewCanvas
                        )
                        layoutParam.topMargin = margin.toInt()
                        layoutParam.leftMargin = 0
                        layoutEditing.layoutParams = layoutParam

                    }
                }

            }.onTap(object : OnTapListener {
                override fun onTap(e: MotionEvent?): Boolean {
                    Log.e("OnTAP", "${e?.x}/${e?.y}")
//                    x = e!!.x
//                    y = e!!.y - margin.toInt()
//                    newCanvas?.drawText(
//                        "Hello Minhf laf",
//                        x ,
//                        y,
//                        paint
//                    )
                    return false
                }

            })
                .enableSwipe(false).pages(0).enableDoubletap(false).load()
            pdfView.maxZoom = 1f
        } catch (ex: Exception) {
            Log.e("new activity error", intent.extras!!.getString("file"))
        }
    }

    @Throws(IOException::class)
    private fun saveBitmapToPNG(bitmap: Bitmap, photo: File) {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val stream: OutputStream = FileOutputStream(photo)
        newBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    }

    private fun saveSignature(signature: Bitmap): File? {
        try {
            val output = File.createTempFile("signer", ".png", applicationContext.cacheDir)
            saveBitmapToPNG(signature, output)
            return output
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
