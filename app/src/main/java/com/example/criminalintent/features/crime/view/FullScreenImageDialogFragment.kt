package com.example.criminalintent.features.crime.view

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.criminalintent.databinding.DialogFullscreenImageBinding

class FullScreenImageDialogFragment: DialogFragment()  {
    private var _binding: DialogFullscreenImageBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUri: Uri): FullScreenImageDialogFragment {
            val args = Bundle().apply {
                putParcelable(ARG_IMAGE_URI, imageUri)
            }
            return FullScreenImageDialogFragment().apply { arguments = args }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFullscreenImageBinding.inflate(inflater, container, false)
        Log.d(ARG_IMAGE_URI, "onCreateView")

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getParcelable(ARG_IMAGE_URI, Uri::class.java).let {
            Log.d(ARG_IMAGE_URI, it.toString())
            binding.fullScreenImageView.setImageURI(it)
        }
        binding.closeButton.setOnClickListener { dismiss() }

    }
}