package com.example.storeson


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class Signup : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        val btnCreateAccount = findViewById<Button>(R.id.btnCreateAccount)
        val tvSign = findViewById<TextView>(R.id.tvSign)
        btnCreateAccount.setOnClickListener{
            validateFields()
        }
        tvSign.setOnClickListener{
            startActivity(Intent(this,MainActivity::class.java))
        }

    }
    private fun validateFields(){

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmailAddress = findViewById<EditText>(R.id.etEmailAddress)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        //kullanıcının girdiği bilgiler alınıdı
        val fullName = etFullName.text.toString().trim()
        val email = etEmailAddress.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmpassword = etConfirmPassword.text.toString().trim()

        //Gerekli alanların boş olup olmadığı kontrol edilir
        val fields = listOf(
            etFullName to fullName,
            etEmailAddress to email,
            etPassword to password,
            etConfirmPassword to confirmpassword
        )

        for((editText,value) in fields){
            if (value.isEmpty()){
                // Boş alanları işaretler ve işlem çubuğunu gizler
                editText.error ="${editText.hint} is required"
                progressBar.visibility = View.INVISIBLE
                return
            }
        }

        // Şifre uzunluğu kontrol edilir
        if (password.length<6){
            etPassword.error ="Password must be at least 6 characters"
            progressBar.visibility = View.INVISIBLE
            return
        }

        // Şifrelerin eşleşip eşleşmediği kontrol edilir
        if (password!= confirmpassword){
            etConfirmPassword.error = "Password do not match"
            progressBar.visibility = View.INVISIBLE
            return
        }

        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (!signInMethods.isNullOrEmpty()) {
                        // Kullanıcı zaten kayıtlı ise hata mesajı gösterilir
                        etEmailAddress.error = "Email address is already registered"
                        Toast.makeText(this, "Email address is already registered", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    } else {
                        // Kullanıcı mevcut değilse, kullanıcıyı kaydetmeye devam eder
                        authenticateUser(email, password, fullName)
                    }
                }
            }
    }
    private fun authenticateUser(email: String, password: String, fullName: String) {

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmailAddress = findViewById<EditText>(R.id.etEmailAddress)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val kullanıcıadı = etFullName.text.toString().trim()
        val email = etEmailAddress.text.toString().trim()
        // Firebase Authentication kullanarak kullanıcıyı kaydet
        // Yeni kullanıcı kaydı ve displayName güncelleme
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Kullanıcı başarılı bir şekilde oluşturuldu
                    val user = auth.currentUser

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(kullanıcıadı)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d("Firebase", "User profile updated.")
                            }
                        }
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    // Kullanıcı mevcutsa, kullanıcı ID'si alınır ve cüzdan oluşturulur
                    if (currentUser != null) {
                        Toast.makeText(this,"Creating store is sucsessful",Toast.LENGTH_SHORT).show()
                        val userId: String = currentUser.uid
                        createWallet(userId)
                    }
                } else {
                    // Kullanıcı oluşturulamadı
                    Log.w("Firebase", "createUserWithEmail:failure", task.exception)
                }
            }

    }
    private fun createWallet(userId: String) {
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmailAddress = findViewById<EditText>(R.id.etEmailAddress)
        val kullanıcıadı = etFullName.text.toString().trim()
        val email = etEmailAddress.text.toString().trim()
        // Firestore kullanarak kullanıcıya ait bir cüzdan oluştur
        val db = FirebaseFirestore.getInstance()
        //val walletsCollection = db.collection("wallets")

        // Yeni bir cüzdan belgesi oluşturulur
        val walletData = hashMapOf(
            "email" to email,
            "name" to kullanıcıadı, // Başlangıç bakiyesi 0 olarak ayarlanır
            "uid" to userId // Boş bir işlem geçmişi oluşturulur
        )

        db.collection("business").document(userId)
            .set(walletData)
            .addOnSuccessListener {
                Toast.makeText(this,"Creating store wallets succesful",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,Mainpage::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,"Creating store wallets failed",Toast.LENGTH_SHORT).show()

            }

    }



}