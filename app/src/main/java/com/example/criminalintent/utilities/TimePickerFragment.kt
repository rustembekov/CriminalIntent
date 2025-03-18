package com.example.criminalintent.utilities


import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar


class TimePickerFragment : DialogFragment() {
    companion object {
        private const val TIME_PICKER_FRAGMENT = "time_picker_fragment"
        private const val TIME_PICKER_DATE = "time_picker_date"
        private const val REQUEST_TIME_PICKER = "REQUEST_TIME_PICKER"


        fun newInstance(date: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(TIME_PICKER_DATE, date)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(TIME_PICKER_DATE, Date::class.java) ?: Date()
        Log.d(TIME_PICKER_FRAGMENT, "Received Date = $date")

        val calendar = Calendar.getInstance().apply { time = date }
        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)

        val timeListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            val resultDate = GregorianCalendar(
                initialYear,
                initialMonth,
                initialDay,
                hour,
                minute
            ).time
            Log.d(TIME_PICKER_FRAGMENT, resultDate.toString())

            parentFragmentManager.setFragmentResult(
                REQUEST_TIME_PICKER,
                bundleOf(REQUEST_TIME_PICKER to resultDate)
            )
        }
        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHour,
            initialMinute,
            true
        )
    }

}