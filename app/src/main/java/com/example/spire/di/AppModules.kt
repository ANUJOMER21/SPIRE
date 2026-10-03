package com.example.spire.di

import androidx.room.Room
import com.example.spire.BuildConfig
import com.example.spire.core.AndroidConnectivityObserver
import com.example.spire.core.ConnectivityObserver
import com.example.spire.data.local.MIGRATION_1_2
import com.example.spire.data.local.SpireDatabase
import com.example.spire.data.remote.DummyJsonApi
import com.example.spire.data.repository.CartRepositoryImpl
import com.example.spire.data.repository.ProductRepositoryImpl
import com.example.spire.domain.repository.CartRepository
import com.example.spire.domain.repository.ProductRepository
import com.example.spire.ui.MainViewModel
import com.example.spire.ui.cart.CartViewModel
import com.example.spire.ui.detail.ProductDetailViewModel
import com.example.spire.ui.products.ProductListViewModel
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val networkModule = module {
    single { Json { ignoreUnknownKeys = true; coerceInputValues = true } }
    single {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(DummyJsonApi.BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DummyJsonApi::class.java)
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), SpireDatabase::class.java, "spire.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    single { get<SpireDatabase>().productDao() }
    single { get<SpireDatabase>().cartDao() }
}

val repositoryModule = module {
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    single<ProductRepository> { ProductRepositoryImpl(get(), get()) }
    single<CartRepository> { CartRepositoryImpl(get()) }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { ProductListViewModel(get(), get(), get(), get()) }
    viewModel { params -> ProductDetailViewModel(params.get(), get(), get()) }
    viewModel { CartViewModel(get()) }
}

val appModules = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
