# A-variant candidates (protocol A4)

Campaign valid runs on disk: 100 of 108 (50% mark = 54). Order = run start time. port_hint = imports from the app's own package in the flagged file (lower is closer to a mechanical port).


## P08: 8 candidate file(s) in 8 run(s)

### P08 #1: agy-g38flash-B1-naive-r1 app/src/main/java/com/example/librarycatalogue/ui/components/BookCover.kt:216 (P08, port_hint 2)
gradient: Brush.horizontalGradient( colors = listOf( in list item scope BookSpineOverlay

~~~kotlin
 210          val w = size.width
 211          val h = size.height
 212          val spineWidth = w * 0.08f
 213  
 214          // Spine shadow gradient
 215          drawRect(
 216>             brush = Brush.horizontalGradient(
 217                  colors = listOf(
 218                      Color.Black.copy(alpha = 0.45f),
 219                      Color.Black.copy(alpha = 0.1f),
 220                      Color.White.copy(alpha = 0.15f),
 221                      Color.Transparent
 222                  ),
~~~

### P08 #2: agy-g38flash-B2-naive-r1 app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:251 (P08, port_hint 10)
shadow>2dp: defaultElevation = 6.dp) ) { Column( modifier in list item scope DigitalLibraryCard

~~~kotlin
 245  @Composable
 246  fun DigitalLibraryCard(member: MemberDto) {
 247      Card(
 248          modifier = Modifier.fillMaxWidth(),
 249          shape = RoundedCornerShape(16.dp),
 250          colors = CardDefaults.cardColors(containerColor = NavyDark),
 251>         elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
 252      ) {
 253          Column(
 254              modifier = Modifier.padding(20.dp)
 255          ) {
 256              Row(
 257                  modifier = Modifier.fillMaxWidth(),
~~~

### P08 #3: agy-g38flash-B3-naive-r2 app/src/main/java/com/example/libraryloans/ui/components/BookCoverView.kt:32 45 46 (P08, port_hint 1)
gradient: Brush.verticalGradient( colors = listOf( baseColor in list item scope BookCoverView

~~~kotlin
  26  @Composable
  27  fun BookCoverView(
  28      book: Book,
  29      modifier: Modifier = Modifier
  30  ) {
  31      val baseColor = Color(book.coverAccentHex)
  32>     val darkGradient = Brush.verticalGradient(
  33          colors = listOf(
  34              baseColor,
  35              baseColor.copy(alpha = 0.85f),
  36              Color(0xFF0F172A).copy(alpha = 0.95f)
  37          )
  38      )
~~~

### P08 #4: agy-g38flash-B2-naive-r2 app/src/main/java/com/library/app/ui/profile/ProfileScreen.kt:193 199 (P08, port_hint 6)
shadow>2dp: defaultElevation = 6.dp) ) { Box( modifier = M in list item scope DigitalLibraryCard

~~~kotlin
 187  fun DigitalLibraryCard(profile: MemberProfile) {
 188      Card(
 189          modifier = Modifier
 190              .fillMaxWidth()
 191              .height(200.dp),
 192          shape = RoundedCornerShape(16.dp),
 193>         elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
 194      ) {
 195          Box(
 196              modifier = Modifier
 197                  .fillMaxSize()
 198                  .background(
 199                      Brush.horizontalGradient(
~~~


## P06: 8 candidate file(s) in 8 run(s)

### P06 #1: codex-luna-B3-detailed-functional-r1 app/src/main/java/vn/shelftap/app/MainActivity.kt:39 (P06, port_hint 1)
BitmapFactory.decodeStream without inSampleSize (full-resolution decode)

~~~kotlin
  33  class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{ShelfTapApp()}}}
  34  private val green=Color(0xFF315F51);private val dateFmt=DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
  35  private fun date(s:String)=runCatching{dateFmt.format(Instant.parse(s))}.getOrDefault(s.take(10));private fun days(s:String)=runCatching{ChronoUnit.DAYS.between
  36  @Composable fun ShelfTapApp(vm:MainViewModel=viewModel()){val st by vm.state.collectAsState();var tab by remember{mutableIntStateOf(0)};var detail by remember{m
  37  @Composable fun LoansScreen(st:AppState,vm:MainViewModel,onDetail:(String)->Unit){if(st.loansOffline)SavedNote();if(st.loading&&st.loans.isEmpty())Loading();Laz
  38  @Composable fun HistoryScreen(st:AppState,vm:MainViewModel,onDetail:(String)->Unit){var confirm by remember{mutableStateOf(false)};Row(Modifier.fillMaxWidth().p
  39> @Composable fun DetailScreen(d:TitleDetails?,id:String,onBack:()->Unit){var bitmap by remember(d?.coverUrl){mutableStateOf<Bitmap?>(null)};LaunchedEffect(d?.cov
  40  @Composable fun SavedNote(){Text("Showing saved data",Modifier.fillMaxWidth().background(Color(0xFFE3F1EA)).padding(12.dp),color=green)}
  41  @Composable fun Loading(){Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
~~~

### P06 #2: codex-luna-B1-guardrailed-r2 app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:131 (P06, port_hint 3)
AsyncImage without a size-bounding modifier (decodes at original size)

~~~kotlin
 125          }
 126      }
 127  }
 128  
 129  @Composable
 130  private fun Cover(title: TitleEntity, modifier: Modifier) {
 131>     if (title.coverUrl?.startsWith("https://") == true) AsyncImage(model = title.coverUrl, contentDescription = stringResource(R.string.cover_description, title
 132      else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_cover), style
 133  }
~~~

### P06 #3: claude-haiku-B1-guardrailed-r2 app/src/main/java/vn/shelftap/app/ui/common/TitleRow.kt:96 (P06, port_hint 2)
AsyncImage without a size-bounding modifier (decodes at original size)

~~~kotlin
  90                  style = MaterialTheme.typography.labelSmall,
  91                  textAlign = TextAlign.Center,
  92                  modifier = Modifier.padding(4.dp)
  93              )
  94          }
  95      } else {
  96>         AsyncImage(
  97              model = ImageRequest.Builder(LocalContext.current)
  98                  .data(coverUrl)
  99                  .crossfade(true)
 100                  .build(),
 101              contentDescription = contentDescription,
 102              contentScale = ContentScale.Crop,
~~~

### P06 #4: agy-g38flash-B3-detailed-functional-r3 app/src/main/java/vn/shelftap/app/ui/components/CommonComponents.kt:102 (P06, port_hint 1)
AsyncImage without a size-bounding modifier (decodes at original size)

~~~kotlin
  96  @Composable
  97  fun BookCoverImage(
  98      coverUrl: String?,
  99      contentDescription: String?,
 100      modifier: Modifier = Modifier
 101  ) {
 102>     AsyncImage(
 103          model = ImageRequest.Builder(LocalContext.current)
 104              .data(coverUrl)
 105              .crossfade(true)
 106              .build(),
 107          placeholder = painterResource(R.drawable.ic_book_placeholder),
 108          error = painterResource(R.drawable.ic_book_placeholder),
~~~


## P01+P09: 16 candidate file(s) in 16 run(s)

### P01+P09 #1: claude-haiku-B3-detailed-functional-r1 app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt:105 (P01, port_hint 4)
items(state.items) in LazyColumn has no key

~~~kotlin
  99                      style = MaterialTheme.typography.bodyMedium,
 100                  )
 101              }
 102          } else if (!state.loading && state.error == null && state.items.isEmpty()) {
 103              item { Text("No past loans yet.", style = MaterialTheme.typography.bodyMedium) }
 104          }
 105>         items(state.items) { item ->
 106              HistoryCard(item = item, onOpen = { onOpenTitle(item.titleId) })
 107          }
 108      }
 109  }
 110  
 111  @Composable
~~~

### P01+P09 #2: agy-g38flash-B1-naive-r1 app/src/main/java/com/example/librarycatalogue/ui/components/GenreFilterRow.kt:28 (P01, port_hint 1)
items(Genres.ALL) in LazyRow has no key

~~~kotlin
  22  ) {
  23      LazyRow(
  24          modifier = modifier.fillMaxWidth(),
  25          contentPadding = PaddingValues(horizontal = 16.dp),
  26          horizontalArrangement = Arrangement.spacedBy(8.dp)
  27      ) {
  28>         items(Genres.ALL) { genre ->
  29              val isSelected = genre == selectedGenre
  30              FilterChip(
  31                  selected = isSelected,
  32                  onClick = { onGenreSelected(genre) },
  33                  label = {
  34                      Text(
~~~

### P01+P09 #3: agy-g38flash-B3-naive-r1 app/src/main/java/com/example/libraryloans/ui/components/BookDetailSheet.kt:300 323 (P09, port_hint 9)
Column+scroll iterates (p)) pastLoans.forEach { pastLoan -> Column()

~~~kotlin
 294                          color = MaterialTheme.colorScheme.onSurface
 295                      )
 296                  }
 297  
 298                  Spacer(modifier = Modifier.height(8.dp))
 299  
 300>                 pastLoans.forEach { pastLoan ->
 301                      Column(
 302                          modifier = Modifier
 303                              .fillMaxWidth()
 304                              .padding(vertical = 4.dp)
 305                              .clip(RoundedCornerShape(8.dp))
 306                              .background(Slate100)
~~~

### P01+P09 #4: agy-g38flash-B3-detailed-functional-r1 app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt:195 (P01, port_hint 4)
items(uiState.historyItems) in LazyColumn has no key

~~~kotlin
 189                  LazyColumn(
 190                      modifier = Modifier
 191                          .fillMaxSize()
 192                          .padding(horizontal = 16.dp, vertical = 8.dp),
 193                      verticalArrangement = Arrangement.spacedBy(10.dp)
 194                  ) {
 195>                     items(uiState.historyItems) { item ->
 196                          HistoryRowItem(
 197                              item = item,
 198                              onItemClick = { onTitleClick(item.titleId) }
 199                          )
 200                      }
 201                  }
~~~


## P13: 15 candidate file(s) in 15 run(s)

### P13 #1: claude-haiku-B1-detailed-functional-r1 app/src/main/java/vn/shelftap/app/data/local/TitleDao.kt:17 (P13, port_hint 0)
unindexed LIKE query, no FTS table; debounce=yes

~~~kotlin
  11  interface TitleDao {
  12  
  13      @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
  14      fun pagingAll(): PagingSource<Int, TitleEntity>
  15  
  16      /** [pattern] is already lowercased and escaped with '!' (see CatalogueRepository.likePattern). */
  17>     @Query(
  18          "SELECT * FROM titles WHERE searchText LIKE '%' || :pattern || '%' ESCAPE '!' " +
  19              "ORDER BY title COLLATE NOCASE, id"
  20      )
  21      fun pagingSearch(pattern: String): PagingSource<Int, TitleEntity>
  22  
  23      @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
~~~

### P13 #2: agy-g38flash-B1-naive-r1 app/src/main/java/com/example/librarycatalogue/data/local/BookDao.kt:16 26 (P13, port_hint 0)
unindexed LIKE query, no FTS table; debounce=yes

~~~kotlin
  10  @Dao
  11  interface BookDao {
  12  
  13      @Query("SELECT * FROM books ORDER BY id ASC")
  14      fun getAllBooksPaged(): PagingSource<Int, BookEntity>
  15  
  16>     @Query("""
  17          SELECT * FROM books 
  18          WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%')
  19          ORDER BY id ASC
  20      """)
  21      fun searchBooksPaged(query: String): PagingSource<Int, BookEntity>
  22  
~~~

### P13 #3: agy-g38flash-B3-naive-r1 app/src/main/java/com/example/libraryloans/ui/viewmodel/LoansViewModel.kt:87 97 (P13, port_hint 3)
linear filter over the list on the search query, no index; debounce=no

~~~kotlin
  81              }
  82              .sortedByDescending { it.loan.returnDate }
  83  
  84          val filteredActive = if (query.isBlank()) {
  85              activeLoanItems
  86          } else {
  87>             activeLoanItems.filter {
  88                  it.book.title.contains(query, ignoreCase = true) ||
  89                  it.book.author.contains(query, ignoreCase = true) ||
  90                  it.book.genre.contains(query, ignoreCase = true)
  91              }
  92          }
  93  
~~~

### P13 #4: codex-luna-B1-naive-r1 app/src/main/java/com/bookhaven/catalogue/MainActivity.kt:169 (P13, port_hint 0)
linear filter over the list on the search query, no index; debounce=no

~~~kotlin
 163          c.drawText(book.author, coverX, y + dp(221f), paint)
 164          paint.color = 0xFF6257D9.toInt(); paint.textSize = dp(10f); paint.typeface = Typeface.DEFAULT_BOLD
 165          c.drawText(book.genre.uppercase(), coverX, y + dp(241f), paint)
 166      }
 167  
 168      private fun drawEmpty(c: Canvas) { paint.color = 0xFF697586.toInt(); paint.textAlign = Paint.Align.CENTER; paint.textSize = dp(15f); c.drawText("No books fo
 169>     private fun filteredBooks() = if (query.isBlank()) allBooks else allBooks.filter { "${it.title} ${it.author} ${it.genre}".contains(query, true) }
 170  
 171      override fun onTouchEvent(event: MotionEvent): Boolean {
 172          when (event.action) {
 173              MotionEvent.ACTION_DOWN -> { downY = event.y; lastY = event.y; moved = false; return true }
 174              MotionEvent.ACTION_MOVE -> { val delta = event.y - lastY; if (kotlin.math.abs(event.y - downY) > dp(5f)) moved = true; scrollOffset -= delta; lastY 
 175              MotionEvent.ACTION_UP -> { performClick(); return true }
~~~


## P03+P04: 23 candidate file(s) in 11 run(s)

### P03+P04 #1: agy-g38flash-B3-naive-r1 app/src/main/java/com/example/libraryloans/ui/components/BookDetailSheet.kt:58 65 67 (P03+P04, port_hint 9)
fun BookDetailSheet(loansForBook: List<Loan>) unstable parameter

~~~kotlin
  52  import com.example.libraryloans.ui.theme.Slate800
  53  import com.example.libraryloans.ui.viewmodel.LoanWithBook
  54  import java.time.format.DateTimeFormatter
  55  
  56  @OptIn(ExperimentalMaterial3Api::class)
  57  @Composable
  58> fun BookDetailSheet(
  59      book: Book,
  60      loansForBook: List<Loan>,
  61      onDismiss: () -> Unit,
  62      onRenewLoan: (LoanWithBook) -> Unit
  63  ) {
  64      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
~~~

### P03+P04 #2: agy-g38flash-B3-naive-r1 app/src/main/java/com/example/libraryloans/ui/components/HistoryItemCard.kt:51 (P04, port_hint 5)
DateTimeFormatter.ofPattern("MMM dd, yyyy") Card( modifier = modifier in composable HistoryItemCard without remember

~~~kotlin
  45      item: LoanWithBook,
  46      onDetailsClick: () -> Unit,
  47      modifier: Modifier = Modifier
  48  ) {
  49      val loan = item.loan
  50      val book = item.book
  51>     val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
  52  
  53      Card(
  54          modifier = modifier
  55              .fillMaxWidth()
  56              .clickable { onDetailsClick() },
  57          shape = RoundedCornerShape(16.dp),
~~~

### P03+P04 #3: agy-g38flash-B3-naive-r1 app/src/main/java/com/example/libraryloans/ui/components/LoanItemCard.kt:65 (P04, port_hint 11)
DateTimeFormatter.ofPattern("MMM dd, yyyy") val (badgeBg, badgeFg, statusLabel, statu in composable LoanItemCard without remember

~~~kotlin
  59      modifier: Modifier = Modifier
  60  ) {
  61      val loan = item.loan
  62      val book = item.book
  63      val status = loan.getEffectiveStatus()
  64      val days = loan.daysRemaining()
  65>     val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
  66  
  67      val (badgeBg, badgeFg, statusLabel, statusIcon) = when (status) {
  68          LoanStatus.OVERDUE -> Quad(
  69              OverdueRedLight,
  70              OverdueRed,
  71              "Overdue by ${abs(days)} ${if (abs(days) == 1L) "day" else "days"}",
~~~

### P03+P04 #4: agy-g38flash-B3-naive-r1 app/src/main/java/com/example/libraryloans/ui/components/RenewConfirmDialog.kt:48 (P04, port_hint 5)
DateTimeFormatter.ofPattern("MMM dd, yyyy") val baseDate = if (loan.dueDate.isBefore( in composable RenewConfirmDialog without remember

~~~kotlin
  42      item: LoanWithBook,
  43      onConfirm: (loanId: String) -> Unit,
  44      onDismiss: () -> Unit
  45  ) {
  46      val loan = item.loan
  47      val book = item.book
  48>     val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
  49  
  50      val baseDate = if (loan.dueDate.isBefore(LocalDate.now())) LocalDate.now() else loan.dueDate
  51      val proposedDueDate = baseDate.plusDays(14)
  52      val remainingRenewalsAfter = loan.maxRenewals - (loan.renewalCount + 1)
  53  
  54      AlertDialog(
~~~
