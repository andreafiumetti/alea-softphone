/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.main.fragment

import android.content.res.Configuration
import android.graphics.Outline
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.core.view.doOnPreDraw
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.slidingpanelayout.widget.SlidingPaneLayout.PanelSlideListener
import com.google.android.material.textfield.TextInputLayout
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.LinphoneApplication.Companion.corePreferences
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.BottomNavBarBinding
import org.linphone.databinding.MainActivityTopBarBinding
import org.linphone.ui.main.MainActivity
import org.linphone.ui.main.chat.fragment.ConversationsListFragmentDirections
import org.linphone.ui.main.contacts.fragment.ContactsListFragmentDirections
import org.linphone.ui.main.history.fragment.HistoryListFragmentDirections
import org.linphone.ui.main.meetings.fragment.MeetingsListFragmentDirections
import org.linphone.ui.main.viewmodel.AbstractMainViewModel
import org.linphone.utils.Event
import org.linphone.utils.SlidingPaneBackPressedCallback
import org.linphone.utils.hideKeyboard
import org.linphone.utils.setKeyboardInsetListener
import org.linphone.utils.showKeyboard

@UiThread
abstract class AbstractMainFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Abstract Main Fragment]"

        private const val TIME_MS_AFTER_WHICH_REFRESH_DATA_ON_RESUME = 3600000 // 1 hour
    }

    protected val outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            val radius = resources.getDimension(R.dimen.top_bar_rounded_corner_radius)
            view ?: return
            outline?.setRoundRect(0, 0, view.width, (view.height + radius).toInt(), radius)
        }
    }

    protected var lastOnPauseTimestamp: Long = -1L

    private var currentFragmentId: Int = 0

    private lateinit var navigationBar: View

    private lateinit var viewModel: AbstractMainViewModel

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (viewModel.searchBarVisible.value == true) {
                viewModel.closeSearchBar()
                return
            }

            Log.i("$TAG Search bar is closed, going back")
            isEnabled = false
            try {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (ise: IllegalStateException) {
                Log.w("$TAG Can't go back: $ise")
            }
        }
    }

    abstract fun onDefaultAccountChanged()

    abstract fun onSlidingPaneClosed()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )

        lastOnPauseTimestamp = -1
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onPause() {
        lastOnPauseTimestamp = System.currentTimeMillis()
        super.onPause()
    }

    fun shouldRefreshDataInOnResume(): Boolean {
        if (lastOnPauseTimestamp == -1L) return false
        if (!corePreferences.keepServiceAlive) return false
        return System.currentTimeMillis() - lastOnPauseTimestamp > TIME_MS_AFTER_WHICH_REFRESH_DATA_ON_RESUME
    }

    fun setViewModel(abstractMainViewModel: AbstractMainViewModel) {
        (view?.parent as? ViewGroup)?.doOnPreDraw {
            startPostponedEnterTransition()
        }

        viewModel = abstractMainViewModel

        viewModel.openDrawerMenuEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as MainActivity).toggleDrawerMenu()
            }
        }

        viewModel.searchFilter.observe(viewLifecycleOwner) { filter ->
            viewModel.applyFilter(filter.trim())
        }

        viewModel.missedCallsCount.observe(viewLifecycleOwner) {
            sharedViewModel.refreshDrawerMenuAccountsListEvent.value = Event(false)
        }

        viewModel.navigateToContactsEvent.observe(viewLifecycleOwner) {
            it.consume {
                if (currentFragmentId != R.id.contactsListFragment) {
                    goToContactsList()
                }
            }
        }

        viewModel.navigateToHistoryEvent.observe(viewLifecycleOwner) {
            it.consume {
                if (currentFragmentId != R.id.historyListFragment) {
                    goToHistoryList()
                }
            }
        }

        viewModel.navigateToDialpadEvent.observe(viewLifecycleOwner) {
            it.consume {
                openDialpad()
            }
        }

        viewModel.navigateToVoicemailEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialVoicemail()
            }
        }

        viewModel.navigateToConversationsEvent.observe(viewLifecycleOwner) {
            it.consume {
                if (currentFragmentId != R.id.conversationsListFragment) {
                    goToConversationsList()
                }
            }
        }

        viewModel.navigateToMeetingsEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialVoicemail()
            }
        }

        viewModel.defaultAccountChangedEvent.observe(viewLifecycleOwner) {
            it.consume {
                onDefaultAccountChanged()
            }
        }

        sharedViewModel.currentlyDisplayedFragment.observe(viewLifecycleOwner) {
            viewModel.contactsSelected.value = it == R.id.contactsListFragment
            viewModel.callsSelected.value = it == R.id.historyListFragment
            viewModel.dialpadSelected.value = it == R.id.startCallFragment
            viewModel.conversationsSelected.value = it == R.id.conversationsListFragment
            viewModel.meetingsSelected.value = false
        }

        sharedViewModel.resetMissedCallsCountEvent.observe(viewLifecycleOwner) {
            it.consume {
                viewModel.resetMissedCallsCount()
            }
        }

        sharedViewModel.forceUpdateAvailableNavigationItems.observe(viewLifecycleOwner) {
            it.consume {
                coreContext.postOnCoreThread {
                    viewModel.updateAvailableMenus()
                }
            }
        }
    }

    fun initViews(
        slidingPane: SlidingPaneLayout,
        topBar: MainActivityTopBarBinding,
        navBar: BottomNavBarBinding,
        @IdRes fragmentId: Int
    ) {
        navigationBar = navBar.root

        initSlidingPane(slidingPane)
        initSearchBar(topBar.search)
        initNavigation(fragmentId)
    }

    private fun initSlidingPane(slidingPane: SlidingPaneLayout) {
        val slidingPaneBackPressedCallback = SlidingPaneBackPressedCallback(slidingPane)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            slidingPaneBackPressedCallback
        )

        view?.doOnPreDraw {
            slidingPane.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
            val slideable = slidingPane.isSlideable
            sharedViewModel.isSlidingPaneSlideable.value = slideable
            slidingPaneBackPressedCallback.isEnabled = slideable && slidingPane.isOpen
            Log.d("$TAG Sliding Pane is ${if (slideable) "slideable" else "flat"}")
            // Tablet/large: se non c'è dettaglio aperto, lista a tutta larghezza
            if (!slideable && !slidingPane.isOpen) {
                applyDualPaneListExpanded(slidingPane, expanded = true)
            }
        }

        sharedViewModel.closeSlidingPaneEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                ensureNavigationBarIsVisible()
                onSlidingPaneClosed()
                if (slidingPane.isSlideable) {
                    Log.d("$TAG Closing sliding pane")
                    if (!slidingPane.closePane()) {
                        Log.d("$TAG Failed to close sliding pane!")
                        coreContext.postOnMainThreadDelayed({
                            Log.d("$TAG Trying once again to close sliding pane")
                            slidingPane.close()
                        }, 200)
                    }
                } else {
                    applyDualPaneListExpanded(slidingPane, expanded = true)
                }
            }
        }

        sharedViewModel.openSlidingPaneEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (slidingPane.isSlideable && viewModel.searchBarVisible.value == true) {
                    viewModel.focusSearchBarEvent.value = Event(false)
                }

                if (!slidingPane.isSlideable) {
                    applyDualPaneListExpanded(slidingPane, expanded = false)
                }

                if (!slidingPane.isOpen) {
                    Log.d("$TAG Opening sliding pane")
                    if (slidingPane.isSlideable && viewModel.searchBarVisible.value == true) {
                        slidingPane.addPanelSlideListener(object : PanelSlideListener {
                            override fun onPanelSlide(
                                panel: View,
                                slideOffset: Float
                            ) { }

                            override fun onPanelOpened(panel: View) {
                                Log.d("$TAG Closing search bar")
                                viewModel.closeSearchBar()
                                slidingPane.removePanelSlideListener(this)
                            }

                            override fun onPanelClosed(panel: View) {
                                ensureNavigationBarIsVisible()
                            }
                        })
                    }
                    slidingPane.openPane()
                }
            }
        }
    }

    /**
     * Su schermi larghi (pane non slideable) evita la colonna bianca vuota:
     * lista full-width finché non si apre un dettaglio; poi split master/detail.
     * Su smartphone (slideable) non fa nulla — comportamento overlay nativo.
     */
    private fun applyDualPaneListExpanded(slidingPane: SlidingPaneLayout, expanded: Boolean) {
        if (slidingPane.isSlideable || slidingPane.childCount < 2) return
        val list = slidingPane.getChildAt(0)
        val detail = slidingPane.getChildAt(1)
        val lp = list.layoutParams as SlidingPaneLayout.LayoutParams
        if (expanded) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            detail.visibility = View.GONE
            Log.d("$TAG Dual-pane: list expanded (no detail)")
        } else {
            lp.width = resources.getDimensionPixelSize(R.dimen.sliding_pane_left_fragment_width)
            detail.visibility = View.VISIBLE
            Log.d("$TAG Dual-pane: master/detail split")
        }
        list.layoutParams = lp
        slidingPane.requestLayout()
    }

    private fun initSearchBar(searchBar: TextInputLayout) {
        searchBar.editText?.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                view.hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }

        viewModel.searchBarVisible.observe(viewLifecycleOwner) { visible ->
            backPressedCallback.isEnabled = visible
        }

        viewModel.focusSearchBarEvent.observe(viewLifecycleOwner) {
            it.consume { show ->
                if (show) {
                    // To automatically open keyboard
                    searchBar.showKeyboard()
                } else {
                    searchBar.hideKeyboard()
                    ensureNavigationBarIsVisible()
                }
            }
        }

        searchBar.setKeyboardInsetListener { keyboardVisible ->
            val portraitOrientation = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            navigationBar.visibility = if (!portraitOrientation || !keyboardVisible) View.VISIBLE else View.GONE
        }
    }

    private fun ensureNavigationBarIsVisible() {
        if (::navigationBar.isInitialized) {
            navigationBar.visibility = View.VISIBLE
        }
    }

    private fun initNavigation(@IdRes fragmentId: Int) {
        currentFragmentId = fragmentId

        sharedViewModel.navigateToContactsEvent.observe(viewLifecycleOwner) {
            it.consume {
                goToContactsList()
            }
        }

        sharedViewModel.navigateToHistoryEvent.observe(viewLifecycleOwner) {
            it.consume {
                goToHistoryList()
            }
        }

        sharedViewModel.navigateToConversationsEvent.observe(viewLifecycleOwner) {
            it.consume {
                goToConversationsList()
            }
        }

        sharedViewModel.navigateToMeetingsEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialVoicemail()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (currentFragmentId > 0) {
            sharedViewModel.currentlyDisplayedFragment.value = currentFragmentId
        }
    }

    private fun goToContactsList() {
        Log.i("$TAG Navigating to contacts list")
        when (currentFragmentId) {
            R.id.conversationsListFragment -> {
                Log.i("$TAG Leaving conversations list")
                val action = ConversationsListFragmentDirections.actionConversationsListFragmentToContactsListFragment()
                navigateTo(action)
            }
            R.id.meetingsListFragment -> {
                Log.i("$TAG Leaving meetings list")
                val action = MeetingsListFragmentDirections.actionMeetingsListFragmentToContactsListFragment()
                navigateTo(action)
            }
            R.id.historyListFragment -> {
                Log.i("$TAG Leaving history list")
                val action = HistoryListFragmentDirections.actionHistoryListFragmentToContactsListFragment()
                navigateTo(action)
            }
        }
    }

    private fun goToHistoryList() {
        Log.i("$TAG Navigating to history list")
        when (currentFragmentId) {
            R.id.conversationsListFragment -> {
                Log.i("$TAG Leaving conversations list")
                val action = ConversationsListFragmentDirections.actionConversationsListFragmentToHistoryListFragment()
                navigateTo(action)
            }
            R.id.contactsListFragment -> {
                Log.i("$TAG Leaving contacts list")
                val action = ContactsListFragmentDirections.actionContactsListFragmentToHistoryListFragment()
                navigateTo(action)
            }
            R.id.meetingsListFragment -> {
                Log.i("$TAG Leaving meetings list")
                val action = MeetingsListFragmentDirections.actionMeetingsListFragmentToHistoryListFragment()
                navigateTo(action)
            }
        }
    }

    private fun goToConversationsList() {
        Log.i("$TAG Navigating to conversations list")
        when (currentFragmentId) {
            R.id.contactsListFragment -> {
                Log.i("$TAG Leaving contacts list")
                val action = ContactsListFragmentDirections.actionContactsListFragmentToConversationsListFragment()
                navigateTo(action)
            }
            R.id.meetingsListFragment -> {
                Log.i("$TAG Leaving meetings list")
                val action = MeetingsListFragmentDirections.actionMeetingsListFragmentToConversationsListFragment()
                navigateTo(action)
            }
            R.id.historyListFragment -> {
                Log.i("$TAG Leaving history list")
                val action = HistoryListFragmentDirections.actionHistoryListFragmentToConversationsListFragment()
                navigateTo(action)
            }
        }
    }

    private fun goToMeetingsList() {
        Log.i("$TAG Navigating to meetings list")
        when (currentFragmentId) {
            R.id.conversationsListFragment -> {
                Log.i("$TAG Leaving conversations list")
                val action = ConversationsListFragmentDirections.actionConversationsListFragmentToMeetingsListFragment()
                navigateTo(action)
            }
            R.id.contactsListFragment -> {
                Log.i("$TAG Leaving contacts list")
                val action = ContactsListFragmentDirections.actionContactsListFragmentToMeetingsListFragment()
                navigateTo(action)
            }
            R.id.historyListFragment -> {
                Log.i("$TAG Leaving history list")
                val action = HistoryListFragmentDirections.actionHistoryListFragmentToMeetingsListFragment()
                navigateTo(action)
            }
        }
    }

    private fun openDialpad() {
        Log.i("$TAG Opening dialpad")
        val dest = findNavController().currentDestination?.id
        if (dest == R.id.startCallFragment) {
            viewModel.dialpadSelected.value = true
            return
        }
        if (dest != R.id.historyListFragment && currentFragmentId != R.id.historyListFragment) {
            goToHistoryList()
        }
        sharedViewModel.openDialpadEvent.value = Event(true)
    }

    private fun dialVoicemail() {
        Log.i("$TAG Dialing voicemail *97")
        coreContext.postOnCoreThread { core ->
            val address = core.interpretUrl("*97", true) ?: return@postOnCoreThread
            coreContext.startCall(address)
        }
    }

    private fun navigateTo(action: NavDirections) {
        try {
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("$TAG Failed to navigate: $e")
        }
    }
}
