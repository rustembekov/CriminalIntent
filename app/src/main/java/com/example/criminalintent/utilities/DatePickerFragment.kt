package com.example.criminalintent.utilities

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

class DatePickerFragment : DialogFragment() {

    companion object {
        private const val REQUEST_DATE_PICKER = "REQUEST_DATE_PICKER"
        private const val ARG_DATE_PICKER = "date_picker"
        private const val TIME_PICKER_FRAGMENT = "time_picker"


        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE_PICKER, date)
            }
            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(ARG_DATE_PICKER, Date::class.java) ?: Date()
        val calendar = Calendar.getInstance().apply { time = date }
        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val resultDate = GregorianCalendar(year, month, day).time

            parentFragmentManager.setFragmentResult(
                REQUEST_DATE_PICKER,
                bundleOf(REQUEST_DATE_PICKER to resultDate)
            )

            TimePickerFragment.newInstance(resultDate).apply {
                show(this@DatePickerFragment.parentFragmentManager, TIME_PICKER_FRAGMENT)
            }

        }

        return DatePickerDialog(
            requireContext(),
            dateListener,
            initialYear,
            initialMonth,
            initialDay
        )
    }

}
