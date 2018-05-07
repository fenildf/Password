package ruolan.com.password.mvp.presenter

import ruolan.com.password.model.PasswordModel
import ruolan.com.password.mvp.view.EditDetailView
import ruolan.com.password.service.impl.EditServiceImpl
import ruolan.com.uselibrary.presenter.BasePresenter
import javax.inject.Inject

open class EditDetailPresenter @Inject constructor(): BasePresenter<EditDetailView>() {

    @Inject
    lateinit var service: EditServiceImpl

    fun onEditDetail(model:PasswordModel){

        mView.showLoading()

        //执行逻辑

        if(true){
            mView.onEditSucc()
        } else {
            mView.onEditFailure()
        }


    }


}