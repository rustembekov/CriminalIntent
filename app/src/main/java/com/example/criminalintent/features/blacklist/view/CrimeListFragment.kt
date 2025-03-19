package com.example.criminalintent.features.blacklist.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.R
import com.example.criminalintent.callbacks.CrimeCallback
import com.example.criminalintent.features.blacklist.viewModel.CrimeListViewModel
import com.example.criminalintent.model.Crime

private const val TAG = "CrimeListFragment"
private const val BLANK_FRAGMENT_TAG = "BLANK_FRAGMENT_TAG"

class CrimeListFragment : Fragment() {
    private lateinit var crimeRecyclerView: RecyclerView
    private val crimeAdapter = CrimeAdapter()
    private var callbacks: CrimeCallback? = null
    private val crimeListViewModel:
            CrimeListViewModel by lazy {
        ViewModelProvider(this)[CrimeListViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as CrimeCallback?
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Total crimes: ${crimeListViewModel.state.value?.crimesList}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view)
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = crimeAdapter
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        crimeListViewModel.state.value?.crimesList?.observe(viewLifecycleOwner) { crimes ->
            if (crimes.isNullOrEmpty()) {
                showBlankListFragment()
            } else {
                showCrimeList()
                crimeAdapter.submitList(crimes)
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crime_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when(menuItem.itemId) {
                    R.id.new_crime -> {
                        val crime = Crime()
                        crimeListViewModel.addCrime(crime)
                        callbacks?.onCrimeSelected(crime.id)
                        true
                    } else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showBlankListFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, BlankListFragment.newInstance(), BLANK_FRAGMENT_TAG)
            .commit()
    }

    private fun showCrimeList() {
        crimeRecyclerView.visibility = View.VISIBLE
    }

    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var crime: Crime
        val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        val crimeSolvedTextView: TextView = itemView.findViewById(R.id.crime_solved)
        val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        val crimePoliceButton: Button = itemView.findViewById(R.id.crime_police)
        private val solvedImageView: ImageView? = itemView.findViewById(R.id.crime_solved_image)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = crime.title
            dateTextView.text = crime.date.toString()
            solvedImageView?.visibility = if (crime.isSolved) View.VISIBLE else View.GONE
            crimeSolvedTextView.visibility = if (crime.isSolved) View.VISIBLE else View.GONE
            crimePoliceButton.visibility = if (crime.isSolved) View.GONE else View.VISIBLE
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crimeId = crime.id)
        }
    }

    private inner class CrimeAdapter : ListAdapter<Crime, CrimeHolder>(CrimeDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}

