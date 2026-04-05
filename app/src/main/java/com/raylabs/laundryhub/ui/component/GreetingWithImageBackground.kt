package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.DateUtil

@Composable
fun GreetingWithImageBackground(
    username: String,
    imageSeed: String
) {
    val todayKey = remember { DateUtil.getTodayDate("yyyy-MM-dd") }
    val backgroundImageUrl = remember(imageSeed, todayKey) {
        val stableSeed = "$imageSeed-$todayKey".hashCode().toLong() and 0x7fffffff
        val imageId = ((stableSeed % 999L) + 1L).toInt()
        "https://picsum.photos/id/$imageId/800/300"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        AsyncImage(
            model = backgroundImageUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.gradient_img),
            alpha = 0.5f,
            error = painterResource(R.drawable.gradient_img),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(vertical = 30.dp, horizontal = 16.dp)) {
            Text(stringResource(R.string.hello), style = MaterialTheme.typography.body1)
            Text(username, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        }
    }
}
