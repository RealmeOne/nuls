webpackJsonp([12],{"0dGN":function(e,t){},ap1E:function(e,t,a){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var s=a("LPk9"),l=a("6ROu"),r=a.n(l),i=a("x47x"),n={data:function(){return{address:this.$route.params.address,freezeData:[],totalAll:0,pageNumber:1,freezeSetInterval:null}},components:{Back:s.a},mounted:function(){var e=this;this.getLocked("/accountledger/utxo/lock/"+this.address,{pageSize:20,pageNumber:1}),this.freezeSetInterval=setInterval(function(){e.getLocked("/accountledger/utxo/lock/"+e.address,{pageSize:20,pageNumber:e.pageNumber})},2e3)},destroyed:function(){clearInterval(this.freezeSetInterval)},methods:{getLocked:function(e,t){var a=this;this.$fetch(e,t).then(function(e){if(e.success){var t=new i.BigNumber(1e-8);a.totalAll=e.data.total;for(var s=0;s<e.data.list.length;s++)e.data.list[s].value=parseFloat(t.times(e.data.list[s].value).toString()),e.data.list[s].createTime=r()(e.data.list[s].createTime).format("YYYY-MM-DD HH:mm:ss"),e.data.list[s].lockTimes=e.data.list[s].lockTime,e.data.list[s].lockTime>1e12&&(console.log(e.data.list[s].lockTime>1e12),e.data.list[s].lockTimes=r()(e.data.list[s].lockTime).format("YYYY-MM-DD HH:mm:ss"));a.freezeData=e.data.list}else a.freezeData=[]})},freezeSize:function(e){this.pageNumber=e,this.getLocked("/accountledger/utxo/lock/"+this.address,{pageSize:20,pageNumber:e})}}},o={render:function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("div",{staticClass:"freeze-list"},[a("Back",{attrs:{backTitle:this.$t("message.accountManagement")}}),e._v(" "),a("div",{staticClass:"freeze-list-tabs"},[a("h2",[e._v(e._s(e.$t("message.freezeList")))]),e._v(" "),a("el-table",{attrs:{data:e.freezeData}},[a("el-table-column",{attrs:{prop:"txType",label:e.$t("message.type"),"min-width":"60",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return[e._v("\n                    "+e._s(e.$t("message.type"+t.row.txType))+"\n                ")]}}])}),e._v(" "),a("el-table-column",{attrs:{prop:"value",label:e.$t("message.amount"),width:"150",align:"right"}}),e._v(" "),a("el-table-column",{attrs:{prop:"createTime",label:e.$t("message.freezeTime"),"min-width":"100",align:"center"}}),e._v(" "),a("el-table-column",{attrs:{prop:"lockTime",label:e.$t("message.thawingTime"),"min-width":"100",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return[e._v("\n                    "+e._s(parseInt(t.row.lockTime)>1e12?t.row.lockTimes:-1===parseInt(t.row.lockTime)?e.$t("message.c158"):e.$t("message.c139")+t.row.lockTime)+"\n                ")]}}])})],1),e._v(" "),a("el-pagination",{directives:[{name:"show",rawName:"v-show",value:e.totalAllOk=this.totalAll>20,expression:"totalAllOk = this.totalAll>20 ? true:false"}],staticClass:"cb",attrs:{layout:"prev, pager, next","page-size":20,total:this.totalAll},on:{"current-change":e.freezeSize}})],1)],1)},staticRenderFns:[]};var c=a("vSla")(n,o,!1,function(e){a("0dGN")},null,null);t.default=c.exports}});