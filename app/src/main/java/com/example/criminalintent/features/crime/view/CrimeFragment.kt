package com.example.criminalintent.features.crime.view

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.R
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

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }

    companion object {
        private const val TAG = "CrimeFragment"

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
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
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
