package com.example.criminalintent.features.crime.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.R
import com.example.criminalintent.features.crime.viewModel.CrimeDetailViewModel
import com.example.criminalintent.model.Crime
import com.example.criminalintent.utilities.DatePickerFragment
import com.example.criminalintent.utilities.getScaledBitmap
import java.io.File
import java.util.UUID

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }
    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleCapturedImage()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }


    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { contactUri ->
                    fetchContactName(contactUri)?.let { suspect ->
                        crime.suspect = suspect
                        crimeDetailViewModel.saveCrime(crime)
                        suspectButton.text = suspect
                    }
                }
            }
        }

    private val callContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { contactUri ->
                    fetchContactNumber(contactUri)?.let { phoneNumber -> dialNumber(phoneNumber) }
                }
            }
        }



    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                requireContext(),
                "Permission required to access contacts",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    companion object {
        private const val DATE_FORMAT = "EEE, MMM dd"

        fun newInstance(crimeId: UUID) = CrimeFragment().apply {
            arguments = Bundle().apply { putSerializable(Args.CRIME_ID, crimeId) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CRIME", "onCreate!")

        crime = Crime()
        arguments?.getSerializable(Args.CRIME_ID, UUID::class.java)
            ?.let { crimeDetailViewModel.loadCrime(it) }

    }

    override fun onDetach() {
        super.onDetach()
        Log.d("CRIME", "onDetachView!")

        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("CRIME", "onCreateView!")

        return inflater.inflate(R.layout.fragment_crime, container, false).apply {
            titleField = findViewById(R.id.crime_title)
            dateButton = findViewById(R.id.crime_date)
            solvedCheckBox = findViewById(R.id.crime_solved)
            reportButton = findViewById(R.id.crime_report)
            suspectButton = findViewById(R.id.crime_suspect)
            callButton = findViewById(R.id.crime_call)
            photoButton = findViewById(R.id.crime_camera)
            photoView = findViewById(R.id.crime_photo)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) {
            it?.let {
                crime = it
                photoFile = crimeDetailViewModel.getPhotoCrime(it)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.example.android.criminalintent.fileprovider",
                    photoFile
                )
                updateUI()
                updatePhotoView()  // Ensure the photo is set
            }
        }
    }


    override fun onStart() {
        super.onStart()
        Log.d("CRIME", "onStart!")

        setupUIListeners()
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun setupUIListeners() {
        setupDateButton()
        setupTitleFieldListener()
        setupSolvedCheckBox()
        setupReportButton()
        setupSuspectButton()
        setupCallButton()
        setupPhotoButton()
        setupPhotoView()
    }

    private fun setupDateButton() {
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).show(parentFragmentManager, Args.DIALOG_DATE)
        }
    }

    private fun setupTitleFieldListener() {
        titleField.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
        })
    }

    private fun setupSolvedCheckBox() {
        solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
        }
    }

    private fun setupReportButton() {
        reportButton.setOnClickListener { sendCrimeReport() }
    }

    private fun setupSuspectButton() {
        suspectButton.setOnClickListener {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            pickContactLauncher.launch(pickContactIntent)
        }
    }

    private fun setupCallButton() {
        callButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                callContactLauncher.launch(pickContactIntent)
            } else {
                requestReadContactsPermission()
            }
        }
    }

    private fun setupPhotoButton() {
        photoButton.apply {
            if(!checkIfDeviceHasCamera())
                isEnabled = false

            setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }

            }
        }
    }
    private fun checkIfDeviceHasCamera(): Boolean {
        return requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }


    private fun grantUriPermissions(intent: Intent) {
        val cameraActivities = requireActivity().packageManager.queryIntentActivities(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        for (cameraActivity in cameraActivities) {
            requireActivity().grantUriPermission(
                cameraActivity.activityInfo.packageName,
                photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun setupPhotoView() {
        photoView.viewTreeObserver.addOnGlobalLayoutListener {
            val drawable = photoView.drawable
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                Log.d("Drawable.bitmap", "setupPhotoView")
                photoView.setOnClickListener {
                    FullScreenImageDialogFragment.newInstance(photoUri)
                        .show(parentFragmentManager, Args.DIALOG_IMAGE_FULLSCREEN)
                }
            }
        }

    }

    private fun handleCapturedImage() {
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        updatePhotoView()
        Toast.makeText(requireContext(), "Image saved!", Toast.LENGTH_SHORT).show()
    }

    private fun requestReadContactsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun sendCrimeReport() {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getCrimeReport())
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
        }.also { intent ->
            startActivity(Intent.createChooser(intent, getString(R.string.send_report)))
        }
    }

    private fun getCrimeReport(): String {
        val solvedString =
            if (crime.isSolved) getString(R.string.crime_report_solved) else getString(R.string.crime_report_unsolved)
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect =
            crime.suspect.takeIf { it.isNotBlank() } ?: getString(R.string.crime_report_no_suspect)
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    private fun fetchContactName(uri: Uri): String? {
        return requireActivity().contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun fetchContactNumber(uri: Uri): String? {
        return requireActivity().contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts._ID),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getString(0)
                requireActivity().contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                    ?.use { phoneCursor -> if (phoneCursor.moveToFirst()) phoneCursor.getString(0) else null }
            } else null
        }
    }

    private fun dialNumber(phoneNumber: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.isChecked = crime.isSolved
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }

    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            Log.d("Photo.bitmap", "photoView: ${photoView.drawable.toString()}")

        } else {
            Log.d("Photo.bitmap", "null")
            photoView.setImageDrawable(null)
        }
    }

    private fun launchCamera() {
        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        grantUriPermissions(captureImageIntent)
        captureImageLauncher.launch(captureImageIntent)
    }
}

open class SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable?) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}

object Args {
    const val CRIME_ID = "crime_id"
    const val DIALOG_DATE = "DialogDate"
    const val DIALOG_IMAGE_FULLSCREEN = "DialogImage"
}
