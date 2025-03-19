package com.example.criminalintent.features.blacklist.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.R
import com.example.criminalintent.callbacks.CrimeCallback
import com.example.criminalintent.features.blacklist.viewModel.CrimeListViewModel
import com.example.criminalintent.model.Crime

class BlankListFragment : Fragment() {
    companion object {
        fun newInstance(): BlankListFragment {
            return BlankListFragment()
        }
    }

    private var callbacks: CrimeCallback? = null
    private val crimeListViewModel:
            CrimeListViewModel by lazy {
        ViewModelProvider(this)[CrimeListViewModel::class.java]
    }
    private lateinit var imageTextView: ImageView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CrimeCallback) {
            callbacks = context
        } else {
            throw IllegalStateException("Host activity must implement CrimeCallback")
        }
    }


    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.blank_list_content, container, false)
        imageTextView = view.findViewById(R.id.ic_blank_content)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onStart() {
        super.onStart()

        imageTextView.setOnClickListener {
            val crime = Crime()
            crimeListViewModel.addCrime(crime)
            callbacks?.onCrimeSelected(crime.id)
        }
    }
}