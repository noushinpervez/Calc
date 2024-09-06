package edu.ewubd.calc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.mariuszgromada.math.mxparser.Expression;

public class MainActivity extends AppCompatActivity {

    private EditText etInput;
    private TextView tvResult;
    private boolean fromEqualBtn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.input);
        tvResult = findViewById(R.id.result);

        // disable keyboard popup
        etInput.setShowSoftInputOnFocus(false);

        setButtonClickListeners();

        findViewById(R.id.btnAc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFields();
            }
        });

        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteChar();
            }
        });

        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromEqualBtn = true;
                calculateResult();
            }
        });
    }

    private void clearFields() {
        etInput.setText("");
        tvResult.setText("");
    }

    private void deleteChar() {
        int curPosition = etInput.getSelectionStart();
        String curText = etInput.getText().toString();

        // remove the character at the cursor position
        if (curPosition > 0 && !curText.isEmpty()) {
            String leftStr = curText.substring(0, curPosition - 1);
            String rightStr = curText.substring(curPosition);
            etInput.setText(String.format("%s%s", leftStr, rightStr));
            etInput.setSelection(curPosition - 1);
        }

        if (etInput.getText().length() > 0) calculateResult();
        else clearFields();
    }

    private void setButtonClickListeners() {
        View.OnClickListener buttonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String buttonText = button.getText().toString();
                String curText = etInput.getText().toString();
                int curPosition = etInput.getSelectionStart();
                char lastChar = curPosition > 0 ? curText.charAt(curPosition - 1) : '\0';

                // conditions for skipping input
                // if input is empty and button is an operator or ')' at start, do nothing
                if (curText.isEmpty() && (buttonText.equals(")") || isOperator(buttonText))) return;

                // if there is an operator before ')' and button is ')', do nothing
                if (isOperator(String.valueOf(lastChar)) && buttonText.equals(")") && lastChar != '%')
                    return;

                // if an operator is clicked right after an opening parenthesis, do nothing
                if (isOperator(buttonText) && curPosition > 0 && curText.charAt(curPosition - 1) == '(')
                    return;

                // prevent recurrence of operators for valid expression
                if (isOperator(buttonText) && isOperator(String.valueOf(curText.charAt(curText.length() - 1)))) {
                    String leftStr = curText.substring(0, curPosition - 1);
                    String rightStr = curText.substring(curPosition);
                    etInput.setText(String.format("%s%s%s", leftStr, buttonText, rightStr));
                    etInput.setSelection(curPosition);
                    calculateResult();
                    return;
                }

                // prevent addition of extra closing brackets to maintain balanced parentheses
                if (buttonText.equals(")")) {
                    int openBracket = 0;
                    int closeBracket = 0;
                    for (char c : curText.toCharArray()) {
                        if (c == '(') openBracket++;
                        else if (c == ')') closeBracket++;
                    }
                    if (closeBracket >= openBracket) return;
                }

                etInput.getText().insert(curPosition, buttonText);
                calculateResult();
            }
        };

        // set click listener for each button
        int[] buttonIds = {R.id.btnZero, R.id.btnOne, R.id.btnTwo, R.id.btnThree, R.id.btnFour, R.id.btnFive, R.id.btnSix, R.id.btnSeven, R.id.btnEight, R.id.btnNine, R.id.btnPoint, R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide, R.id.btnOpenBracket, R.id.btnCloseBracket, R.id.btnPi, R.id.btnPercent};
        for (int buttonId : buttonIds)
            findViewById(buttonId).setOnClickListener(buttonClickListener);
    }

    private boolean isOperator(String str) {
        return str.matches("[+\\-×÷.%]");
    }

    private void calculateResult() {
        String result;
        String userExp = etInput.getText().toString().trim();

        if (userExp.length() > 1) {
            String leftStr = userExp.substring(0, userExp.length() - 1);
            // remove trailing operators for calculation
            while (!userExp.isEmpty()) {
                char lastChar = userExp.charAt(userExp.length() - 1);
                if (lastChar == '.' || lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷')
                    userExp = leftStr;
                break;
            }
        }

        // replace symbols for calculation
        userExp = userExp.replaceAll("÷", "/");
        userExp = userExp.replaceAll("×", "*");
        userExp = userExp.replaceAll("π(\\d)", "π*$1");
        userExp = userExp.replaceAll("(\\d+\\.?\\d*)%([\\d.]+|π)", "$1%*$2");

        // add necessary closing brackets at the end to balance the equation
        int openBracket = 0;
        int closeBracket = 0;
        for (int i = 0; i < userExp.length(); i++) {
            if (userExp.charAt(i) == '(') openBracket++;
            else if (userExp.charAt(i) == ')') closeBracket++;
        }
        if (openBracket > closeBracket) userExp += ")".repeat(openBracket - closeBracket);

        // check for division by zero
        if (userExp.contains("/0")) {
            tvResult.setText("Can't divide by 0");
            setError();
            return;
        }

        // calculate expression
        Expression exp;
        exp = new Expression(userExp);
        double resultValue = exp.calculate();

        // handle result and error messages
        if (Double.isNaN(resultValue)) {
            tvResult.setText("");
            return;
        } else {
            // format result with 10 decimal places and trim trailing zeros
            result = String.format("%.10f", resultValue).replaceAll("\\.?0*$", "");
            resetColor();
        }
        tvResult.setText(result);

        // if calculation was triggered by Equal button, update input field with result
        if (fromEqualBtn) {
            etInput.setText(result);
            etInput.setSelection(etInput.getText().length());
            tvResult.setText("");
            fromEqualBtn = false;
        }
    }

    private void setError() {
        int error = ContextCompat.getColor(this, R.color.error);
        etInput.setTextColor(error);
        tvResult.setTextColor(error);
    }

    private void resetColor() {
        int text = ContextCompat.getColor(this, R.color.text);
        etInput.setTextColor(text);
        tvResult.setTextColor(text);
    }
}