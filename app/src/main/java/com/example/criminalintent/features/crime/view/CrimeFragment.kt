package com.example.criminalintent.features.crime.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.R
import com.example.criminalintent.features.blacklist.view.CrimeListFragment
import com.example.criminalintent.features.crime.viewModel.CrimeDetailViewModel
import com.example.criminalintent.model.Crime
import com.example.criminalintent.utilities.DatePickerFragment
import java.util.Date
import java.util.UUID

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }

    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
            contactUri?.let {
                fetchContactName(it)?.let { suspect ->
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
        }

    private val callContactLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
            contactUri?.let {
                fetchContactNumber(it)?.let { phoneNumber -> dialNumber(phoneNumber) }
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
        crime = Crime()
        arguments?.getSerializable(Args.CRIME_ID, UUID::class.java)
            ?.let { crimeDetailViewModel.loadCrime(it) }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_crime, container, false).apply {
            titleField = findViewById(R.id.crime_title)
            dateButton = findViewById(R.id.crime_date)
            solvedCheckBox = findViewById(R.id.crime_solved)
            reportButton = findViewById(R.id.crime_report)
            suspectButton = findViewById(R.id.crime_suspect)
            callButton = findViewById(R.id.crime_call)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) {
            it?.let {
                crime = it; updateUI()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setupUIListeners()
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun setupUIListeners() {
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).show(parentFragmentManager, Args.DIALOG_DATE)
        }

        titleField.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
        })

        solvedCheckBox.setOnCheckedChangeListener { _, isChecked -> crime.isSolved = isChecked }
        reportButton.setOnClickListener { sendCrimeReport() }
        suspectButton.setOnClickListener { pickContactLauncher.launch(null) }
        callButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            )
                callContactLauncher.launch(null)
            else {
                requestReadContactsPermission()
            }
        }
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
    const val REQUEST_DATE_PICKER = "REQUEST_DATE_PICKER"
}
