package com.bitpunchlab.android.shareroutes

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

class UserInfo : BaseObservable() {

    @get:Bindable
    var name: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.name)
        }

    @get:Bindable
    var email: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.email)
        }

    @get:Bindable
    var password: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.password)
        }

    @get:Bindable
    var confirmPassword: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.confirmPassword)
        }
}

class UserEmail : BaseObservable() {
    @get:Bindable
    var email2: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.email2)
        }

}