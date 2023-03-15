<template>
  <div class="demo-image__error">
    <div class="block">
      <span class="demonstration">简易图片剪切板</span>
      <el-image v-if="imgUrl" :src="imgUrl" :key="imgUrl" :fit="'scale-down'"/>
      <el-upload
          class="upload-demo"
          :action="'http://' + host + ':' + port + '/api/picture'"
          :on-success="uploadSucceed"
          :on-error="uploadFailed"
          accept="image/png, image/jpeg"
          :show-file-list="false"
      >
        <el-button type="primary">Click to upload</el-button>
      </el-upload>
    </div>
  </div>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-input
          v-model="textarea1"
          :rows="4"
          type="textarea"
          placeholder="文字剪切板1号"
          clearable
      />
    </el-col>
    <el-col :span="6">
      <el-button type="success" @click="postText(0, textarea1)">提交</el-button>
    </el-col>
  </el-row>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-input
          v-model="textarea2"
          :rows="4"
          type="textarea"
          placeholder="文字剪切板2号"
          clearable
      />
    </el-col>
    <el-col :span="6">
      <el-button type="success" @click="postText(1, textarea2)">提交</el-button>
    </el-col>
  </el-row>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-input
          v-model="textarea3"
          :rows="4"
          type="textarea"
          placeholder="文字剪切板3号"
          clearable
      />
    </el-col>
    <el-col :span="6">
      <el-button type="success" @click="postText(2, textarea3)">提交</el-button>
    </el-col>
  </el-row>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-input
          v-model="textarea4"
          :rows="4"
          type="textarea"
          placeholder="文字剪切板4号"
          clearable
      />
    </el-col>
    <el-col :span="6">
      <el-button type="success" @click="postText(3, textarea4)">提交</el-button>
    </el-col>
  </el-row>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-input
          v-model="textarea5"
          :rows="4"
          type="textarea"
          placeholder="文字剪切板5号"
          clearable
      />
    </el-col>
    <el-col :span="6">
      <el-button type="success" @click="postText(4, textarea5)">提交</el-button>
    </el-col>
  </el-row>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue'
import {ElMessage} from "element-plus";
import axios from "axios";


export default defineComponent({
  setup() {
    const textarea1 = ref('')
    const textarea2 = ref('')
    const textarea3 = ref('')
    const textarea4 = ref('')
    const textarea5 = ref('')
    const textArray = [textarea1, textarea2, textarea3, textarea4, textarea5]
    const port = process.env.NODE_ENV === 'development' ? '8080' : window.location.port
    const host = window.location.hostname
    const imgUrl = ref('http://' + host + ':' + port + '/api/picture')

    const getText = () => {
      axios.get('http://' + host + ':' + port + '/api/getAll').then(res => {
        const code = res.data.code
        if (code == 200) {
          const data_arr = res.data.msg
          textarea1.value = data_arr[0][0]
          textarea2.value = data_arr[1][0]
          textarea3.value = data_arr[2][0]
          textarea4.value = data_arr[3][0]
          textarea5.value = data_arr[4][0]
        }
        else {
          ElMessage({
            message: 'get text failed',
            type: 'warning',
          })
        }
      })
    }

    const postText = (fromId: number, content: string) => {
      axios.post('http://' + host + ':' + port + '/api/post', {
        fromId: fromId,
        message: content
      }).then(res => {
        const code = res.data.code
        if (code == 200) {
          const data_arr = res.data.msg
          textArray[fromId].value = data_arr[0]
          ElMessage({
            message: '文字同步成功:)',
            type: 'success',
          })
        }
        else {
          ElMessage({
            message: '文字同步失败:(',
            type: 'warning',
          })
        }
      })
    }

    function uploadSucceed() {
      const timestamp = new Date().getTime()
      // to rerender picture
      imgUrl.value = 'http://' + host + ':' + port + '/api/picture' + '?' + timestamp
      ElMessage({
        message: '图片上传成功:)',
        type: 'success',
      })
    }

    function uploadFailed() {
      ElMessage({
        message: '图片上传失败:(',
        type: 'warning',
      })
    }
    getText()

    return {
      textarea1, textarea2, textarea3, textarea4, textarea5, host, port, imgUrl,
      uploadFailed, uploadSucceed, getText, postText
    }
  }

});


</script>

<style scoped>
.demo-image__error .block {
  padding: 30px 0;
  text-align: center;
  border-right: solid 1px var(--el-border-color);
  display: inline-block;
  width: 49%;
  box-sizing: border-box;
  vertical-align: top;
}

.demo-image__error .demonstration {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-bottom: 20px;
}

.demo-image__error .el-image {
  padding: 0 5px;
  max-width: 300px;
  max-height: 200px;
  width: 100%;
  height: 200px;
}

.demo-image__error .image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 30px;
}

.demo-image__error .image-slot .el-icon {
  font-size: 30px;
}
</style>