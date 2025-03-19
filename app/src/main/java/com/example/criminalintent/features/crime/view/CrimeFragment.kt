package com.example.criminalintent.features.crime.view

import android.app.Activity
import android.content.ContentProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }

    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if(data != null) {
                    val contactUri: Uri? = data.data
                    val queryFields = arrayOf(Contacts.DISPLAY_NAME)
                    val cursor = contactUri?.let { requireActivity().contentResolver.query(it, queryFields, null, null, null) }
                    cursor?.use {
                       if(it.count > 0) {
                           it.moveToFirst()
                           val suspect = it.getString(0)
                           crime.suspect = suspect
                           crimeDetailViewModel.saveCrime(crime)
                           suspectButton.text = suspect
                       }

                    }
                }

            }
        }

    companion object {
        private const val TAG = "CrimeFragment"
        private const val DATE_FORMAT = "EEE, MMM, dd"
        private const val REQUEST_CONTACT = 1

        fun newInstance(crimeId: UUID): CrimeFragment {
            return CrimeFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(Args.CRIME_ID, crimeId)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()

        arguments?.getSerializable(Args.CRIME_ID, UUID::class.java)?.let {
            crimeDetailViewModel.loadCrime(it)
            Log.d(TAG, "args bundle crime ID: $it")
        }

        setFragmentResultListeners()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_crime, container, false).apply {
        titleField = findViewById(R.id.crime_title)
        dateButton = findViewById(R.id.crime_date)
        solvedCheckBox = findViewById(R.id.crime_solved)
        reportButton = findViewById(R.id.crime_report)
        suspectButton = findViewById(R.id.crime_suspect)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) { crime ->
            crime?.let {
                this.crime = it
                updateUI()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).show(parentFragmentManager, Args.DIALOG_DATE)
        }

        titleField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
        })

        solvedCheckBox.setOnCheckedChangeListener { _, isChecked -> crime.isSolved = isChecked }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI)

            setOnClickListener {
                pickContactLauncher.launch(pickContactIntent)
//                pickContactIntent.addCategory(Intent.CATEGORY_HOME)
                val packageManager: PackageManager = requireActivity().packageManager
                val resolvedActivity: ResolveInfo? =
                    packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)
                if(resolvedActivity == null) {
                    isEnabled = false
                }

            }
        }

    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDestroy() {
        super.onDestroy()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CrimeListFragment.newInstance())
            .commit()
    }

    private fun getCrimeReport(): String {
        val solvedString = if(crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setFragmentResultListeners() {
        parentFragmentManager.setFragmentResultListener(Args.REQUEST_DATE_PICKER, this) { _, bundle ->
            bundle.getSerializable(Args.REQUEST_DATE_PICKER, Date::class.java)?.let {
                crime.date = it
                updateUI()
            }
        }

        parentFragmentManager.setFragmentResultListener(Args.REQUEST_TIME_PICKER, this) { _, bundle ->
            bundle.getSerializable(Args.REQUEST_TIME_PICKER, Date::class.java)?.let {
                crime.date = it
                updateUI()
            }
        }
    }

    private fun updateUI() {
        with(crime) {
            titleField.setText(title)
            dateButton.text = date.toString()
            solvedCheckBox.apply {
                isChecked = isSolved
                jumpDrawablesToCurrentState()
            }
        }
    }
}

object Args {
    const val CRIME_ID = "crime_id"
    const val DIALOG_DATE = "DialogDate"
    const val REQUEST_DATE_PICKER = "REQUEST_DATE_PICKER"
    const val REQUEST_TIME_PICKER = "REQUEST_TIME_PICKER"
}
