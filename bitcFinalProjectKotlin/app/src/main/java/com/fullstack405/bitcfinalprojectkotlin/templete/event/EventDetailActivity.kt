package com.fullstack405.bitcfinalprojectkotlin.templete.event

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.fullstack405.bitcfinalprojectkotlin.R
import com.fullstack405.bitcfinalprojectkotlin.client.Client
import com.fullstack405.bitcfinalprojectkotlin.data.EventDetailData
import com.fullstack405.bitcfinalprojectkotlin.databinding.ActivityEventDetailBinding
import com.fullstack405.bitcfinalprojectkotlin.templete.QR.QrScannerActivity
import retrofit2.Call
import retrofit2.Response

class EventDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEventDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        setContentView(R.layout.activity_event_detail)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var eventId = intent.getLongExtra("eventId",0)
        var visibleDate = intent.getStringExtra("visibleDate") // 게시일
        var isRegistrationOpen = intent.getCharExtra("isRegistrationOpen",'N') // Y : 진행중 , N:마감
        var userId = intent.getLongExtra("userId",0) // 접속자Id

        // 회원인지 아닌지만 판단
        var userPermission = intent.getStringExtra("userPermission")

        binding.btnQRscanner.isVisible = false

        if (!userPermission.equals("ROLE_REGULAR")) { // 정회원
            binding.btnQRscanner.isVisible = true

            if (isRegistrationOpen == 'N') { //마감된 행사는 비활성화
                binding.btnQRscanner.isEnabled = false
            }
            else{ // 진행중은 버튼 클릭 이벤트
                binding.btnQRscanner.setOnClickListener {
                    startBarcodeScanner()
                }
            }
        } // if permission


        lateinit var event:EventDetailData
        var url = "http://10.100.105.205:8080/eventImg/"
//        var posterName = event.eventPoster
        
//        이벤트id로 해당 이벤트 정보만 불러오기
        Client.retrofit.findEventId(eventId).enqueue(object:retrofit2.Callback<EventDetailData>{
            override fun onResponse(call: Call<EventDetailData>, response: Response<EventDetailData>) {
                Log.d("findEventId","${response.body()}")
                event = response.body() as EventDetailData
                binding.dTitle.text = event.eventTitle
                binding.dContent.text = event.eventContent
                binding.dWriter.text = event.posterUserName
                binding.dCreateDate.text = visibleDate

                // 이미지
                Glide.with(this@EventDetailActivity)
                    .load(url+event.eventPoster)
                    .into(binding.dImage)

            }

            override fun onFailure(call: Call<EventDetailData>, t: Throwable) {
                Log.d("eventDetail error","eventDetail load error")
            }
        }) // retrofit



        /////// 로그인 화면에서부터 userId 계속 들고있어야함
        // 계속 인텐트로 받기 싫으면 shared 사용해야함 방법 찾아보기
        // 신청버튼
        binding.btnSubmit.setOnClickListener {

            // 확인 다이얼로그
            AlertDialog.Builder(this).run{
                setMessage("해당 행사에 참석하시겠습니까?")
                setPositiveButton("확인",object:DialogInterface.OnClickListener{
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        // db 연결버전
                        Client.retrofit.insertEventApp(eventId, userId).enqueue(object:retrofit2.Callback<Int>{
                            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                                Log.d("insert num","${response.body()}")
                                AlertDialog.Builder(this@EventDetailActivity).run {
                                    if(response.body() == 2){
                                        setMessage("신청 완료되었습니다.")
                                    }
                                    else{
                                        setMessage("이미 신청한 행사입니다.")
                                    }

                                    setNegativeButton("닫기",null)
                                    show()
                                }
                            }

                            override fun onFailure(call: Call<Int>, t: Throwable) {
                                Log.d("insert error","${t.message}")
                            }
                        }) // retrofit
                    }// onclick
                }) // positive
                setNegativeButton("취소",null)
                show()
            }
        }




        // 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }



    }// onCreate

    private fun startBarcodeScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        barcodeLauncher.launch(intent)
    }

    private val barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = result.data?.getStringExtra("QR_data") // 인텐트로 받아온 값
            //eventId-scheduleId-userId
            val eventId = scanResult!!.substring(0,1).toLong()
            val scheduleId = scanResult.substring(2,3).toLong()
            val userId = scanResult.substring(4).toLong()

            Toast.makeText(this@EventDetailActivity,"${scanResult}큐알 스캔 성공!",Toast.LENGTH_SHORT).show()
//            Client.retrofit.insertQRCheck(eventId, scheduleId, userId).enqueue(object:retrofit2.Callback<Int>{
//                override fun onResponse(call: Call<Int>, response: Response<Int>) {
//                    Toast.makeText(this@EventDetailActivity,"출석 체크 완료",Toast.LENGTH_SHORT).show()
//                }
//
//                override fun onFailure(call: Call<Int>, t: Throwable) {
//                    Toast.makeText(this@EventDetailActivity,"QR 인증에 실패하였습니다. 다시 확인해주세요.",Toast.LENGTH_SHORT).show()
//                }
//            })
        }else{
            Toast.makeText(this@EventDetailActivity,"실패!!!!!!!!!!",Toast.LENGTH_SHORT).show()
        }
    }
}