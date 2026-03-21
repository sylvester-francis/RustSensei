package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.FlashCard
import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.SpacedRepetition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ReviewUiState(
    val currentCard: FlashCard? = null,
    val isFlipped: Boolean = false,
    val cardsRemaining: Int = 0,
    val cardsReviewed: Int = 0,
    val dueCardCount: Int = 0,
    val totalCardCount: Int = 0,
    val isLoading: Boolean = false,
    val sessionComplete: Boolean = false
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val flashCardDao: FlashCardDao,
    private val contentRepo: ContentRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ReviewViewModel"
    }

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var dueCards = mutableListOf<FlashCard>()
    private var currentIndex = 0

    init {
        // Observe due card count and total card count
        viewModelScope.launch {
            launch {
                flashCardDao.getDueCardCount().collect { count ->
                    _uiState.value = _uiState.value.copy(dueCardCount = count)
                }
            }
            launch {
                flashCardDao.getTotalCardCount().collect { count ->
                    _uiState.value = _uiState.value.copy(totalCardCount = count)
                    // Auto-generate cards from content on first launch
                    if (count == 0) {
                        generateCardsFromContent()
                    }
                }
            }
        }
    }

    fun startReviewSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, sessionComplete = false)
            try {
                val cards = flashCardDao.getDueCards()
                dueCards = cards.toMutableList()
                currentIndex = 0
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cardsRemaining = dueCards.size,
                    cardsReviewed = 0,
                    currentCard = dueCards.firstOrNull(),
                    isFlipped = false,
                    sessionComplete = dueCards.isEmpty()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting review session: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun rateCard(quality: Int) {
        val card = _uiState.value.currentCard ?: return
        viewModelScope.launch {
            try {
                val updatedCard = SpacedRepetition.review(card, quality)
                flashCardDao.update(updatedCard)

                currentIndex++
                val reviewed = _uiState.value.cardsReviewed + 1
                val remaining = dueCards.size - currentIndex

                if (currentIndex < dueCards.size) {
                    _uiState.value = _uiState.value.copy(
                        currentCard = dueCards[currentIndex],
                        isFlipped = false,
                        cardsReviewed = reviewed,
                        cardsRemaining = remaining
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        currentCard = null,
                        isFlipped = false,
                        cardsReviewed = reviewed,
                        cardsRemaining = 0,
                        sessionComplete = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rating card: ${e.message}", e)
            }
        }
    }

    private suspend fun generateCardsFromContent() {
        try {
            // Double-check count to avoid race conditions
            val currentCount = flashCardDao.getTotalCardCount().first()
            if (currentCount > 0) return

            Log.d(TAG, "Generating flashcards from book content...")
            val bookIndex = contentRepo.getBookIndex()
            val allCards = mutableListOf<FlashCard>()

            for (chapter in bookIndex.chapters) {
                val chapterData = contentRepo.getChapter(chapter.id) ?: continue
                for (section in chapterData.sections) {
                    val sectionCategory = deriveCategoryFromChapter(chapter.id)
                    // Generate cards from key terms
                    for (term in section.keyTerms) {
                        val card = generateCardForTerm(term, section.title, sectionCategory, section.id)
                        if (card != null) {
                            allCards.add(card)
                        }
                    }
                }
            }

            if (allCards.isNotEmpty()) {
                flashCardDao.insertAll(allCards)
                Log.d(TAG, "Generated ${allCards.size} flashcards")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating cards from content: ${e.message}", e)
        }
    }

    private fun deriveCategoryFromChapter(chapterId: String): String {
        return when {
            chapterId.contains("ownership") -> "ownership"
            chapterId.contains("common-concepts") -> "basics"
            chapterId.contains("structs") -> "structs"
            chapterId.contains("enums") -> "enums"
            chapterId.contains("packages-crates-modules") -> "modules"
            chapterId.contains("common-collections") -> "collections"
            chapterId.contains("error-handling") -> "error-handling"
            chapterId.contains("generics-traits-lifetimes") -> "generics-traits-lifetimes"
            chapterId.contains("writing-tests") -> "testing"
            chapterId.contains("functional-features") -> "functional"
            chapterId.contains("smart-pointers") -> "smart-pointers"
            chapterId.contains("concurrency") -> "concurrency"
            chapterId.contains("oop") -> "oop"
            chapterId.contains("patterns") -> "patterns"
            chapterId.contains("advanced-features") -> "advanced"
            chapterId.contains("getting-started") -> "getting-started"
            chapterId.contains("guessing-game") -> "getting-started"
            chapterId.contains("io-project") -> "io-project"
            chapterId.contains("cargo") -> "cargo"
            chapterId.contains("web-server") -> "concurrency"
            chapterId.contains("appendix") -> "reference"
            else -> "general"
        }
    }

    private fun generateCardForTerm(
        term: String,
        sectionTitle: String,
        category: String,
        sourceId: String
    ): FlashCard? {
        val qa = termToQA(term, sectionTitle) ?: return null
        return FlashCard(
            front = qa.first,
            back = qa.second,
            category = category,
            sourceId = sourceId
        )
    }

    /**
     * Maps key terms to question/answer pairs.
     * Returns null for terms that don't have meaningful Q&A mappings.
     */
    private fun termToQA(term: String, sectionTitle: String): Pair<String, String>? {
        // Comprehensive term -> Q&A mapping based on Rust book content
        return when (term.lowercase()) {
            // Ownership (ch04)
            "ownership" -> Pair(
                "What is ownership in Rust?",
                "Each value in Rust has exactly one owner. When the owner goes out of scope, the value is dropped. There can only be one owner at a time."
            )
            "stack" -> Pair(
                "What is stored on the stack in Rust?",
                "Data with a known, fixed size at compile time is stored on the stack (LIFO). Pushing to the stack is faster than allocating on the heap."
            )
            "heap" -> Pair(
                "How does heap allocation work in Rust?",
                "Data with unknown or dynamic size is allocated on the heap. The allocator finds space, marks it in use, and returns a pointer. Accessing heap data is slower because you follow a pointer."
            )
            "move" -> Pair(
                "What is a 'move' in Rust?",
                "When you assign a heap-allocated value (like String) to another variable, the data is moved, not copied. The original variable becomes invalid, preventing double-free errors."
            )
            "clone" -> Pair(
                "What does .clone() do in Rust?",
                "clone() creates a deep copy of heap data. Unlike a move, both the original and the copy remain valid. It's an explicit, potentially expensive operation."
            )
            "copy" -> Pair(
                "What is the Copy trait in Rust?",
                "Types implementing Copy (like i32, f64, bool, char) are trivially copied on assignment instead of moved. Only stack-only types can implement Copy."
            )
            "drop" -> Pair(
                "What is the drop function in Rust?",
                "Rust automatically calls drop when a variable goes out of scope, freeing heap memory. This is similar to a destructor in C++ (RAII pattern)."
            )
            "scope" -> Pair(
                "How do scopes work in Rust ownership?",
                "A scope is the range within a program where an item is valid. When a variable's scope ends (closing brace), Rust calls drop and the value is freed."
            )
            "allocator" -> Pair(
                "What role does the memory allocator play in Rust?",
                "The allocator manages heap memory at runtime -- finding space for new data and reclaiming it when dropped. Rust's ownership system determines when to free memory."
            )
            // References & Borrowing (ch04)
            "reference" -> Pair(
                "What is a reference in Rust?",
                "A reference (&T) lets you refer to a value without taking ownership. It's guaranteed to point to a valid value for its lifetime. Created with & operator."
            )
            "borrowing" -> Pair(
                "What is borrowing in Rust?",
                "Borrowing means creating a reference to a value without taking ownership. Like borrowing a book -- you can use it but don't own it and must give it back."
            )
            "mutable reference" -> Pair(
                "What are the rules for mutable references in Rust?",
                "You can have exactly ONE mutable reference (&mut T) to a value at a time. You cannot have mutable and immutable references simultaneously. This prevents data races at compile time."
            )
            "immutable reference" -> Pair(
                "How do immutable references work in Rust?",
                "You can have multiple immutable references (&T) to a value simultaneously, since no one is modifying the data. You cannot have a mutable reference while immutable ones exist."
            )
            "dangling reference" -> Pair(
                "How does Rust prevent dangling references?",
                "The compiler ensures references never outlive the data they point to. If you try to return a reference to a local variable, the compiler will reject it."
            )
            "data race" -> Pair(
                "How does Rust prevent data races?",
                "Rust's borrowing rules prevent data races at compile time: you can't have two mutable references, or a mutable and immutable reference, to the same data simultaneously."
            )
            // Slices (ch04)
            "slice" -> Pair(
                "What is a slice in Rust?",
                "A slice is a reference to a contiguous sequence of elements in a collection. It doesn't have ownership. Written as &[T] for arrays or &str for strings."
            )
            "string slice" -> Pair(
                "What is a string slice (&str)?",
                "A string slice (&str) is a reference to a portion of a String. Created with range syntax: &s[0..5]. String literals are also &str -- immutable references to binary data."
            )
            "&str" -> Pair(
                "What is the &str type in Rust?",
                "&str is a string slice type -- an immutable reference to UTF-8 string data. String literals have type &str. It's a view into a String or static data."
            )
            "range syntax" -> Pair(
                "How does range syntax work for slices in Rust?",
                "Use [start..end] for slices. Shortcuts: [..end] starts at 0, [start..] goes to end, [..] is the whole thing. End index is exclusive."
            )
            "string literal" -> Pair(
                "What type is a string literal in Rust?",
                "String literals (\"hello\") have type &str -- they're slices pointing to data in the program binary. They're always immutable and have a 'static lifetime."
            )
            // Variables & Basics (ch03)
            "variable" -> Pair(
                "How do variables work in Rust?",
                "Variables are immutable by default. Use 'let mut' for mutability. Variables can be shadowed by redeclaring with 'let'. Rust uses type inference."
            )
            "mutability" -> Pair(
                "How does mutability work in Rust?",
                "Variables are immutable by default. Add 'mut' keyword (let mut x = 5) to allow mutation. This is a deliberate design choice for safety."
            )
            "shadowing" -> Pair(
                "What is variable shadowing in Rust?",
                "Shadowing lets you declare a new variable with the same name using 'let'. Unlike mut, shadowing can change the type. The new variable shadows the previous one."
            )
            "constant" -> Pair(
                "How are constants different from immutable variables in Rust?",
                "Constants use 'const' keyword, must have type annotations, must be set to a compile-time expression, and can never be mut. Convention: SCREAMING_SNAKE_CASE."
            )
            "type annotation" -> Pair(
                "When are type annotations required in Rust?",
                "Type annotations are required when the compiler can't infer the type (e.g., parsing strings: let x: u32 = \"42\".parse()). Otherwise, Rust infers types."
            )
            "integer" -> Pair(
                "What integer types does Rust provide?",
                "Signed: i8, i16, i32, i64, i128, isize. Unsigned: u8, u16, u32, u64, u128, usize. Default is i32. isize/usize match platform pointer size."
            )
            "floating-point" -> Pair(
                "What floating-point types does Rust have?",
                "Rust has f32 (single precision) and f64 (double precision). The default is f64 because it's roughly the same speed as f32 on modern CPUs but more precise."
            )
            "tuple" -> Pair(
                "What is a tuple in Rust?",
                "A tuple groups values of different types: let tup: (i32, f64, u8) = (500, 6.4, 1). Access elements with dot notation (tup.0) or destructuring."
            )
            "array" -> Pair(
                "How do arrays work in Rust?",
                "Arrays have fixed length and uniform type: let a: [i32; 5] = [1,2,3,4,5]. Stored on the stack. Rust checks array bounds at runtime, preventing buffer overflows."
            )
            // Functions (ch03)
            "function" -> Pair(
                "How are functions defined in Rust?",
                "Use 'fn' keyword with snake_case names. Parameters must have type annotations. Return type follows '->'. The last expression (without semicolon) is the return value."
            )
            "expression" -> Pair(
                "What's the difference between statements and expressions in Rust?",
                "Statements perform actions and don't return values (let x = 6;). Expressions evaluate to values (5 + 6, blocks {}, function calls). Expressions don't end with semicolons."
            )
            "statement" -> Pair(
                "What is a statement in Rust?",
                "Statements are instructions that perform actions and don't return values. 'let' bindings are statements. Adding a semicolon to an expression turns it into a statement."
            )
            // Control Flow (ch03)
            "if expression" -> Pair(
                "How does 'if' work in Rust?",
                "In Rust, 'if' is an expression that returns a value. Conditions must be bool (no implicit conversion). Can use in let: let x = if cond { 5 } else { 6 };"
            )
            "loop" -> Pair(
                "What loop types does Rust have?",
                "Three loop types: 'loop' (infinite, break to exit, can return values), 'while' (conditional), 'for' (iterate over collections). 'for' is most commonly used."
            )
            // Structs (ch05)
            "struct" -> Pair(
                "How do you define a struct in Rust?",
                "struct User { name: String, age: u32 }. Instantiate with User { name: String::from(\"Alice\"), age: 30 }. Use field init shorthand when variable names match fields."
            )
            "method" -> Pair(
                "How are methods defined on structs in Rust?",
                "Methods are defined in impl blocks: impl MyStruct { fn area(&self) -> u32 { ... } }. First parameter is &self (or &mut self or self). Called with dot notation."
            )
            "associated function" -> Pair(
                "What is an associated function in Rust?",
                "Functions in an impl block that DON'T take self as a parameter. Called with :: syntax (e.g., String::from()). Often used as constructors (Self::new())."
            )
            "field init shorthand" -> Pair(
                "What is field init shorthand in Rust structs?",
                "When variable names match struct field names, you can write User { name, age } instead of User { name: name, age: age }."
            )
            "struct update syntax" -> Pair(
                "What is struct update syntax in Rust?",
                "Create a new struct from an existing one: let user2 = User { email: new_email, ..user1 }. Remaining fields come from user1. Note: this may move user1."
            )
            "tuple struct" -> Pair(
                "What is a tuple struct in Rust?",
                "Tuple structs have named types but unnamed fields: struct Color(i32, i32, i32). Access with .0, .1, etc. Useful for creating distinct types from tuples."
            )
            "unit-like struct" -> Pair(
                "What is a unit-like struct in Rust?",
                "A struct with no fields: struct AlwaysEqual;. Useful for implementing traits on a type without storing data."
            )
            // Enums (ch06)
            "enum" -> Pair(
                "How do enums work in Rust?",
                "Enums define a type with multiple variants: enum IpAddr { V4(String), V6(String) }. Variants can hold different data types. More powerful than enums in most languages."
            )
            "match" -> Pair(
                "How does match work in Rust?",
                "match compares a value against patterns and runs the matching arm's code. Must be exhaustive (cover all cases). Arms can bind to values: Some(val) => val."
            )
            "option" -> Pair(
                "What is Option<T> in Rust?",
                "Option<T> is an enum: Some(T) or None. Rust has no null -- Option forces explicit handling of the 'no value' case, preventing null pointer errors."
            )
            "if let" -> Pair(
                "What is 'if let' in Rust?",
                "if let is syntactic sugar for a match with one pattern: 'if let Some(val) = opt { use val }'. Less verbose than match when you only care about one variant."
            )
            "pattern" -> Pair(
                "What are patterns used for in Rust?",
                "Patterns match against the structure of types. Used in match, if let, while let, for loops, let statements, and function parameters. Can destructure and bind values."
            )
            // Modules (ch07)
            "module" -> Pair(
                "How do modules work in Rust?",
                "Modules organize code with 'mod' keyword. Items are private by default -- use 'pub' for visibility. Nested modules create a tree. Use 'use' to bring items into scope."
            )
            "crate" -> Pair(
                "What is a crate in Rust?",
                "A crate is the smallest compilation unit. Binary crates have main(), library crates don't. The crate root (lib.rs or main.rs) forms the root of the module tree."
            )
            "package" -> Pair(
                "What is a package in Rust?",
                "A package is one or more crates bundled by Cargo.toml. Can have at most one library crate and multiple binary crates. 'cargo new' creates a package."
            )
            "path" -> Pair(
                "How do paths work in Rust's module system?",
                "Absolute paths start from crate root (crate::module::item). Relative paths start from current module (self::, super::). Use 'use' to create shortcuts."
            )
            "use keyword" -> Pair(
                "What does the 'use' keyword do in Rust?",
                "'use' brings module items into scope: use std::collections::HashMap. Convention: bring functions' parent module, but bring structs/enums directly. 'use ... as' for aliases."
            )
            "pub" -> Pair(
                "How does the pub keyword work in Rust?",
                "Items are private by default. 'pub' makes them accessible outside their module. For structs, 'pub' on the struct and each field separately. Enum variants are all public if enum is pub."
            )
            // Collections (ch08)
            "vector" -> Pair(
                "How do vectors (Vec<T>) work in Rust?",
                "Vec<T> is a growable list of same-type values stored on the heap. Create with vec![1,2,3] or Vec::new(). Access with indexing (panics) or .get() (returns Option)."
            )
            "string" -> Pair(
                "What's the difference between String and &str in Rust?",
                "String is an owned, growable, heap-allocated UTF-8 string. &str is a borrowed string slice. String literals are &str. Use String::from() or .to_string() to convert."
            )
            "hash map" -> Pair(
                "How do HashMaps work in Rust?",
                "HashMap<K, V> stores key-value pairs with O(1) lookup. use std::collections::HashMap. Insert with .insert(), access with .get() (returns Option<&V>). Keys must implement Eq + Hash."
            )
            "hashmap" -> Pair(
                "How do you create and use a HashMap in Rust?",
                "use std::collections::HashMap; let mut map = HashMap::new(); map.insert(key, value); Use .entry().or_insert() for conditional insertion."
            )
            // Error Handling (ch09)
            "panic" -> Pair(
                "When should you use panic! in Rust?",
                "panic! is for unrecoverable errors -- it prints an error, unwinds the stack, and exits. Use for programmer bugs, not expected errors. Set RUST_BACKTRACE=1 for details."
            )
            "result" -> Pair(
                "What is Result<T, E> in Rust?",
                "Result<T, E> is an enum: Ok(T) for success, Err(E) for failure. Use match, unwrap(), expect(), or ? operator to handle. Preferred for recoverable errors."
            )
            "? operator" -> Pair(
                "What does the ? operator do in Rust?",
                "The ? operator propagates errors: it returns the Ok value or early-returns the Err from the current function. Only works in functions returning Result or Option."
            )
            "unwrap" -> Pair(
                "What does unwrap() do in Rust?",
                "unwrap() extracts the value from Ok/Some, or panics on Err/None. Use expect(\"msg\") for a better panic message. Avoid in production code -- prefer ? or match."
            )
            // Generics, Traits, Lifetimes (ch10)
            "generic" -> Pair(
                "How do generics work in Rust?",
                "Generics allow type-parameterized code: fn largest<T: PartialOrd>(list: &[T]) -> &T. Rust monomorphizes generics at compile time -- zero runtime cost."
            )
            "trait" -> Pair(
                "What is a trait in Rust?",
                "A trait defines shared behavior (like an interface): trait Summary { fn summarize(&self) -> String; }. Types implement traits with 'impl Trait for Type'. Enables polymorphism."
            )
            "trait bound" -> Pair(
                "What are trait bounds in Rust?",
                "Trait bounds constrain generic types: fn notify(item: &impl Summary) or fn notify<T: Summary>(item: &T). Use + for multiple bounds: T: Display + Clone."
            )
            "lifetime" -> Pair(
                "What are lifetimes in Rust?",
                "Lifetimes ('a) are annotations that tell the compiler how long references are valid. They prevent dangling references. Most lifetimes are inferred; some must be explicit."
            )
            "lifetime annotation" -> Pair(
                "How do lifetime annotations work in Rust?",
                "fn longest<'a>(x: &'a str, y: &'a str) -> &'a str -- means the return reference lives as long as the shorter of x and y. Annotations describe relationships, don't change lifetimes."
            )
            "lifetime elision" -> Pair(
                "What are the lifetime elision rules in Rust?",
                "Three rules the compiler applies: 1) Each reference param gets its own lifetime. 2) If one input lifetime, output gets it. 3) If &self is a param, output gets self's lifetime."
            )
            "'static" -> Pair(
                "What does the 'static lifetime mean in Rust?",
                "'static means the reference can live for the entire program duration. All string literals have 'static lifetime. Often suggested in errors but rarely the right fix."
            )
            "monomorphization" -> Pair(
                "What is monomorphization in Rust?",
                "Monomorphization is Rust's compile-time process of generating specific code for each concrete type used with generics. This means generic code has zero runtime performance cost."
            )
            // Testing (ch11)
            "test" -> Pair(
                "How do you write tests in Rust?",
                "Annotate functions with #[test] in a #[cfg(test)] module. Use assert!, assert_eq!, assert_ne! macros. Run with 'cargo test'. Tests pass if they don't panic."
            )
            "#[test]" -> Pair(
                "What does the #[test] attribute do?",
                "#[test] marks a function as a test function. Cargo runs these when you execute 'cargo test'. Test functions take no arguments and return () or Result<(), E>."
            )
            "assert" -> Pair(
                "What assertion macros does Rust provide for testing?",
                "assert!(bool) -- panics if false. assert_eq!(left, right) -- panics if not equal. assert_ne!(left, right) -- panics if equal. All accept optional format message."
            )
            "#[should_panic]" -> Pair(
                "What does #[should_panic] do in Rust tests?",
                "#[should_panic] makes a test pass if it panics, and fail if it doesn't. Add expected = \"msg\" to check the panic message contains specific text."
            )
            // Closures & Iterators (ch13)
            "closure" -> Pair(
                "What is a closure in Rust?",
                "Closures are anonymous functions that capture their environment: |x| x + 1. They can capture by reference (&T), mutable reference (&mut T), or by value (T). Implement Fn/FnMut/FnOnce."
            )
            "iterator" -> Pair(
                "How do iterators work in Rust?",
                "Iterators implement the Iterator trait with a next() method. They're lazy -- do nothing until consumed. Chain methods like .map(), .filter(), .collect(). Zero-cost abstraction."
            )
            "fn trait" -> Pair(
                "What are the Fn traits in Rust?",
                "FnOnce -- can be called once (takes ownership of captured values). FnMut -- can be called multiple times (mutably borrows). Fn -- can be called multiple times (immutably borrows)."
            )
            "iterator adaptor" -> Pair(
                "What are iterator adaptors in Rust?",
                "Methods that transform iterators into other iterators: .map(), .filter(), .zip(), .enumerate(), .skip(), .take(). They're lazy -- must be consumed (e.g., .collect())."
            )
            "consuming adaptor" -> Pair(
                "What are consuming adaptors in Rust?",
                "Methods that consume the iterator: .collect(), .sum(), .count(), .for_each(), .any(), .all(), .find(). They call next() repeatedly and take ownership of the iterator."
            )
            // Smart Pointers (ch15)
            "box" -> Pair(
                "What is Box<T> in Rust?",
                "Box<T> allocates data on the heap with a pointer on the stack. Used for: unknown-size types at compile time, large data transfer without copying, and trait objects (dyn Trait)."
            )
            "deref trait" -> Pair(
                "What does the Deref trait do in Rust?",
                "Deref lets you customize the * dereference operator. Enables deref coercion: Rust auto-converts &String to &str, &Vec<T> to &[T], etc. Implemented for smart pointers."
            )
            "drop trait" -> Pair(
                "How does the Drop trait work in Rust?",
                "Drop trait lets you customize cleanup when a value goes out of scope. Implements fn drop(&mut self). Called automatically. Use std::mem::drop() for early cleanup."
            )
            "rc" -> Pair(
                "What is Rc<T> in Rust?",
                "Rc<T> (Reference Counted) enables multiple ownership via reference counting. Use Rc::clone() to increment count. When count reaches 0, value is dropped. Single-threaded only."
            )
            "refcell" -> Pair(
                "What is RefCell<T> in Rust?",
                "RefCell<T> enforces borrowing rules at runtime instead of compile time. borrow() returns Ref<T>, borrow_mut() returns RefMut<T>. Panics on rule violations. Single-threaded."
            )
            "interior mutability" -> Pair(
                "What is interior mutability in Rust?",
                "Interior mutability lets you mutate data even through an immutable reference, using unsafe code wrapped in a safe API (RefCell<T>, Cell<T>, Mutex<T>). Checked at runtime."
            )
            "arc" -> Pair(
                "What is Arc<T> in Rust?",
                "Arc<T> (Atomically Reference Counted) is the thread-safe version of Rc<T>. Uses atomic operations for reference counting. Combine with Mutex<T> for shared mutable state across threads."
            )
            "cow" -> Pair(
                "What is Cow<T> in Rust?",
                "Cow<T> (Clone on Write) holds either borrowed or owned data. Clones only when mutation is needed. Useful for functions that sometimes need to modify borrowed data."
            )
            // Concurrency (ch16)
            "thread" -> Pair(
                "How do threads work in Rust?",
                "Create with thread::spawn(|| { ... }). Returns JoinHandle -- call .join() to wait. Use 'move' closures to transfer ownership of captured values into the thread."
            )
            "channel" -> Pair(
                "How do channels work in Rust?",
                "mpsc::channel() returns (tx, rx) -- multiple producer, single consumer. tx.send(val) transfers ownership. rx.recv() blocks, rx.try_recv() doesn't. Clone tx for multiple producers."
            )
            "mutex" -> Pair(
                "How does Mutex<T> work in Rust?",
                "Mutex<T> provides mutual exclusion. .lock() returns a MutexGuard (smart pointer). Lock auto-releases when guard is dropped. Use Arc<Mutex<T>> for sharing across threads."
            )
            "send trait" -> Pair(
                "What is the Send trait in Rust?",
                "Send indicates a type can be transferred between threads. Most types implement Send. Rc<T> does NOT (use Arc<T> instead). Automatically derived by the compiler."
            )
            "sync trait" -> Pair(
                "What is the Sync trait in Rust?",
                "Sync indicates a type can be referenced from multiple threads (&T is Send). Mutex<T> is Sync. RefCell<T> is NOT. Automatically derived by the compiler."
            )
            // OOP (ch17)
            "trait object" -> Pair(
                "What is a trait object in Rust?",
                "A trait object (Box<dyn Trait>) enables dynamic dispatch -- choosing method implementation at runtime. Uses a vtable. Allows different types behind one interface."
            )
            "dynamic dispatch" -> Pair(
                "What is dynamic dispatch in Rust?",
                "Dynamic dispatch (dyn Trait) resolves method calls at runtime via a vtable. Slight runtime cost vs. static dispatch (generics). Required when concrete type isn't known at compile time."
            )
            "static dispatch" -> Pair(
                "What is static dispatch in Rust?",
                "Static dispatch (impl Trait / generics) resolves method calls at compile time via monomorphization. Zero runtime cost but generates more code. Default in Rust."
            )
            // Macros (ch19)
            "macro" -> Pair(
                "What are macros in Rust?",
                "Macros are metaprogramming tools that generate code at compile time. Declarative macros (macro_rules!) match patterns. Procedural macros (derive, attribute, function-like) operate on token streams."
            )
            "derive macro" -> Pair(
                "What are derive macros in Rust?",
                "#[derive(Debug, Clone, PartialEq)] automatically implements traits for structs/enums. Common derives: Debug, Clone, Copy, PartialEq, Eq, Hash, Default."
            )
            // Cargo (ch14)
            "cargo" -> Pair(
                "What is Cargo in Rust?",
                "Cargo is Rust's build system and package manager. Commands: cargo new, cargo build, cargo run, cargo test, cargo doc. Manages dependencies via Cargo.toml."
            )
            "workspace" -> Pair(
                "What is a Cargo workspace?",
                "A workspace is a set of packages sharing one Cargo.lock and output directory. Defined in a root Cargo.toml with [workspace] members. Useful for related crates."
            )
            // Other common terms
            "unsafe" -> Pair(
                "What does unsafe do in Rust?",
                "unsafe opts into five superpowers: dereference raw pointers, call unsafe functions, access mutable statics, implement unsafe traits, access union fields. Doesn't disable the borrow checker."
            )
            "raw pointer" -> Pair(
                "What are raw pointers in Rust?",
                "*const T (immutable) and *mut T (mutable) raw pointers can be null, don't guarantee valid memory, and ignore borrowing rules. Can only be dereferenced in unsafe blocks."
            )
            else -> null
        }
    }
}
