import com.erratick.datawarehouse.auth.UserPasswordEncoderListener
import com.erratick.datawarehouse.load.CommaListValueConverter
import com.erratick.datawarehouse.query.QueryResultRenderer

beans = {
    userPasswordEncoderListener(UserPasswordEncoderListener)
    queryResultRenderer(QueryResultRenderer)
    commaListFormattedValueConverter(CommaListValueConverter)
}