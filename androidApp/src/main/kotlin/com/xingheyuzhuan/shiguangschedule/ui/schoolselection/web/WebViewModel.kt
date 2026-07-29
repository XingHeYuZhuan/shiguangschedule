package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.lifecycle.ViewModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class WebViewModel(
    val courseConversionRepository: CourseConversionRepository
) : ViewModel()