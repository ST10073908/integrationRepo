package com.example.venempoultry

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class FinancialsActivity : AppCompatActivity() {

    private lateinit var financialEntriesContainer: LinearLayout
    private lateinit var database: DatabaseReference
    private lateinit var tabAll: Button
    private lateinit var tabSales: Button
    private lateinit var tabExpenses: Button
    private lateinit var addButton: Button // Declare the add button
    private var allEntries: List<FinancialEntry> = emptyList() // Store all entries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_financials)

        // Initialize views
        financialEntriesContainer = findViewById(R.id.financialEntriesContainer)
        tabAll = findViewById(R.id.tabAll)
        tabSales = findViewById(R.id.tabSales)
        tabExpenses = findViewById(R.id.tabExpenses)
        addButton = findViewById(R.id.addButton) // Initialize the add button

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("financial_entries")

        // Fetch data from Firebase
        fetchFinancialEntries()

        // Setup button listener
        setupAddButton() // Call the method to setup the add button listener

        // Setup tab listeners
        setupTabListeners()
    }

    private fun fetchFinancialEntries() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Map each child to FinancialEntry object
                allEntries = snapshot.children.mapNotNull { it.getValue(FinancialEntry::class.java) }
                displayEntries(allEntries) // Show all entries initially
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FinancialsActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayEntries(entries: List<FinancialEntry>) {
        financialEntriesContainer.removeAllViews() // Clear previous entries

        for (entry in entries) {
            addEntryToContainer(entry)
        }
    }

    private fun addEntryToContainer(entry: FinancialEntry) {
        val entryLayout = LayoutInflater.from(this).inflate(R.layout.financial_entry_item, financialEntriesContainer, false)

        val titleTextView: TextView = entryLayout.findViewById(R.id.tvEntryTitle)
        val categoryTextView: TextView = entryLayout.findViewById(R.id.tvEntryCategory)
        val amountTextView: TextView = entryLayout.findViewById(R.id.tvEntryAmount)

        // Set the text views with entry data
        titleTextView.text = entry.title
        categoryTextView.text = entry.type // Sale or Expense
        amountTextView.text = entry.forPurpose // Assuming this is the amount you want to show

        financialEntriesContainer.addView(entryLayout)
    }

    private fun setupTabListeners() {
        tabAll.setOnClickListener {
            displayEntries(allEntries) // Show all entries
            highlightSelectedTab(tabAll)
        }

        tabSales.setOnClickListener {
            // Filter for Sales entries
            val salesEntries = allEntries.filter { it.type.equals("Sale", ignoreCase = true) }
            displayEntries(salesEntries) // Show only sales entries
            highlightSelectedTab(tabSales)
        }

        tabExpenses.setOnClickListener {
            // Filter for Expense entries
            val expenseEntries = allEntries.filter { it.type.equals("Expense", ignoreCase = true) }
            displayEntries(expenseEntries) // Show only expense entries
            highlightSelectedTab(tabExpenses)
        }
    }

    private fun setupAddButton() {
        addButton.setOnClickListener {
            // Navigate to FinancialsFormActivity
            val intent = Intent(this, FinancialFormActivity::class.java)
            startActivity(intent)
        }
    }

    private fun highlightSelectedTab(selectedTab: Button) {
        // Reset all tab backgrounds
        tabAll.setBackgroundColor(Color.TRANSPARENT)
        tabSales.setBackgroundColor(Color.TRANSPARENT)
        tabExpenses.setBackgroundColor(Color.TRANSPARENT)

        // Highlight the selected tab
        selectedTab.setBackgroundColor(ContextCompat.getColor(this, R.color.black)) // Define this color in your resources
    }
}

// Define a data class to represent a financial entry
data class FinancialEntry(
    val date: String = "",         // Field for date
    val description: String = "",  // Field for description
    val forPurpose: String = "",    // Field for the amount or purpose of the transaction
    val title: String = "",        // Field for the transaction title
    val to: String = "",           // Field for the recipient
    val type: String = ""          // Field for Sale/Expense type
)






class FinancialFormActivity : AppCompatActivity() {

    // Declare views for easier access
    private lateinit var dateInput: EditText
    private lateinit var transactionTitleInput: EditText
    private lateinit var toInput: EditText
    private lateinit var saleExpenseDropdown: Spinner
    private lateinit var forDropdown: Spinner
    private lateinit var descriptionInput: EditText
    private lateinit var submitButton: Button

    // Firebase references
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financials_form) // Adjust as necessary

        // Initialize views
        dateInput = findViewById(R.id.dateInput)
        transactionTitleInput = findViewById(R.id.transactionTitleInput)
        toInput = findViewById(R.id.toInput)
        saleExpenseDropdown = findViewById(R.id.saleExpenseDropdown)
        forDropdown = findViewById(R.id.forDropdown)
        descriptionInput = findViewById(R.id.descriptionInput)
        submitButton = findViewById(R.id.submitButton)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Setup the dropdown menus (Spinners)
        setupDropdowns()

        // Handle form submission
        submitButton.setOnClickListener {
            handleSubmit()
        }

        // Set up date picker
        dateInput.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Format the selected date and set it to dateInput
            val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            dateInput.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun setupDropdowns() {
        // Populate Sale/Expense dropdown
        val saleExpenseOptions = arrayOf("Sale", "Expense")
        val saleExpenseAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            saleExpenseOptions
        )
        saleExpenseDropdown.adapter = saleExpenseAdapter

        // Populate "For" dropdown
        val forOptions = arrayOf("Business", "Personal", "Other")
        val forAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            forOptions
        )
        forDropdown.adapter = forAdapter
    }

    private fun handleSubmit() {
        // Retrieve entered data
        val date = dateInput.text.toString()
        val transactionTitle = transactionTitleInput.text.toString()
        val to = toInput.text.toString()
        val saleOrExpense = saleExpenseDropdown.selectedItem.toString()
        val forPurpose = forDropdown.selectedItem.toString()
        val description = descriptionInput.text.toString()

        // Simple validation
        if (date.isBlank() || transactionTitle.isBlank() || to.isBlank() || description.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a financial entry object
        val financialEntry = FinancialEntry(
            date = date,
            title = transactionTitle,
            to = to,
            type = saleOrExpense,
            forPurpose = forPurpose,
            description = description
        )

        // Get the current user ID
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Save data to Firebase under the user's node in "users/$uid/financials"
            database.child("users").child(userId).child("financials").push().setValue(financialEntry)
                .addOnSuccessListener {
                    Toast.makeText(this, "Financial entry added successfully!", Toast.LENGTH_SHORT).show()
                    clearForm()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add entry: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }




    private fun clearForm() {
        dateInput.text.clear()
        transactionTitleInput.text.clear()
        toInput.text.clear()
        descriptionInput.text.clear()
        saleExpenseDropdown.setSelection(0)
        forDropdown.setSelection(0)
    }

    // Data class for financial entries
    data class FinancialEntry(
        val date: String = "",
        val title: String = "",
        val to: String = "",
        val type: String = "",
        val forPurpose: String = "",
        val description: String = ""
    )
}
