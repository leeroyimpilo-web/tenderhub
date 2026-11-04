package za.co.tenderbase.nativeoffline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TB {
    val background=Color(0xFFF8F7F2)
    val surface=Color.White
    val subtle=Color(0xFFE9EDE2)
    val text=Color(0xFF242821)
    val secondary=Color(0xFF565D50)
    val muted=Color(0xFF62685D)
    val accent=Color(0xFFDDF56B)
    val accentSoft=Color(0xFFEDF6CE)
    val urgent=Color(0xFFAD492F)
    val urgentSoft=Color(0xFFF4DFD6)
    val border=Color(0xFFD4D7CD)
}
val brandFamily=FontFamily(
    Font(R.font.manrope,FontWeight.Normal),
    Font(R.font.manrope,FontWeight.Medium),
    Font(R.font.manrope,FontWeight.SemiBold),
    Font(R.font.manrope,FontWeight.Bold),
    Font(R.font.manrope,FontWeight.ExtraBold)
)
@Composable
fun TenderTheme(content:@Composable ()->Unit){
    val colors=lightColorScheme(primary=TB.text,onPrimary=Color.White,background=TB.background,
        onBackground=TB.text,surface=TB.surface,onSurface=TB.text,
        secondary=TB.secondary,onSecondary=Color.White,error=TB.urgent)
    val typ=Typography(
        displaySmall=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.ExtraBold,fontSize=33.sp,
            lineHeight=36.sp,letterSpacing=(-1.4).sp,color=TB.text),
        headlineMedium=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.ExtraBold,fontSize=30.sp,
            lineHeight=34.sp,letterSpacing=(-1.15).sp,color=TB.text),
        titleLarge=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Bold,fontSize=21.sp,
            lineHeight=26.sp,letterSpacing=(-0.5).sp,color=TB.text),
        titleMedium=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Bold,fontSize=17.sp,
            lineHeight=22.sp,color=TB.text),
        bodyLarge=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Normal,fontSize=16.sp,
            lineHeight=23.sp,color=TB.text),
        bodyMedium=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Normal,fontSize=15.sp,
            lineHeight=21.sp,color=TB.text),
        bodySmall=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Normal,fontSize=13.sp,
            lineHeight=18.sp,color=TB.secondary),
        labelLarge=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Bold,fontSize=14.sp,
            lineHeight=19.sp,color=TB.text),
        labelMedium=TextStyle(fontFamily=brandFamily,fontWeight=FontWeight.Medium,fontSize=12.sp,
            lineHeight=17.sp,color=TB.secondary)
    )
    MaterialTheme(colorScheme=colors,typography=typ,content=content)
}
@Composable
fun BrandMark(modifier:Modifier=Modifier){
    Canvas(modifier.size(27.dp)){val width=size.width;val height=size.height
        val cap=StrokeCap.Square
        val stroke=4.dp.toPx()
        fun line(x1:Float,y1:Float,x2:Float,y2:Float){
            drawLine(TB.accent,Offset(x1,y1),Offset(x2,y2),stroke,cap)
        }
        line(width*.35f,height*.16f,width*.16f,height*.16f)
        line(width*.16f,height*.16f,width*.16f,height*.84f)
        line(width*.16f,height*.84f,width*.35f,height*.84f)
        line(width*.65f,height*.16f,width*.84f,height*.16f)
        line(width*.84f,height*.16f,width*.84f,height*.84f)
        line(width*.84f,height*.84f,width*.65f,height*.84f)
    }
}
@Composable
fun BrandWordmark(){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
        BrandMark()
        Text("tenderbase",style=MaterialTheme.typography.titleLarge,
            fontWeight=FontWeight.ExtraBold,letterSpacing=(-.75).sp)
    }
}
