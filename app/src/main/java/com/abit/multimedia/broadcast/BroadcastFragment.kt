package com.abit.multimedia.broadcast

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.abit.multimedia.R
import com.abit.multimedia.databinding.FragmentBroadcastBinding

/**
 * A. Broadcast Receiver: the first screen of the broadcast flow and the drawer's start screen.
 * The user picks a broadcast type in the Spinner and taps Proceed to open Activity 2.
 */
class BroadcastFragment : Fragment(R.layout.fragment_broadcast) {

    private var _binding: FragmentBroadcastBinding? = null

    // Only valid between onViewCreated() and onDestroyView().
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBroadcastBinding.bind(view)

        setUpSpinner()
        binding.proceedButton.setOnClickListener { openActivity2() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpSpinner() {
        // The ArrayAdapter turns each item of the string-array into a row of the Spinner.
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.broadcast_types,
            R.layout.item_spinner_selected,
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.broadcastTypeSpinner.adapter = adapter
        // The Spinner saves its selected position itself (because it has an android:id), so the
        // choice survives rotation and coming back to this screen through the drawer.

        val descriptions = resources.getStringArray(R.array.broadcast_type_descriptions)
        binding.broadcastTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    _binding?.broadcastTypeDescription?.text = descriptions.getOrNull(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    _binding?.broadcastTypeDescription?.text = null
                }
            }
    }

    private fun openActivity2() {
        val position = binding.broadcastTypeSpinner.selectedItemPosition
        val mode = BroadcastMode.fromSpinnerPosition(position) ?: return
        // Explicit Intent: it names the exact Activity class to start.
        startActivity(BroadcastInputActivity.newIntent(requireContext(), mode))
    }
}
